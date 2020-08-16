package aia.routing

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import akka.actor.AbstractActor.ActorContext
import akka.routing.{ActorRefRoutee, NoRoutee, Pool, Resizer, Routee, Router, SeveralRoutees}
import akka.routing.RoutingLogic
import akka.dispatch.Dispatchers
import akka.remote.ContainerFormats

import scala.concurrent.duration._
import scala.collection.immutable


case class PerformanceRoutingMessage(photo : String, license: Option[String], processedBy: Option[String])

case class SetService(id: String, serviceTime : FiniteDuration)


class GetLicense(pipe: ActorRef, initialServiceTime: FiniteDuration = 0 millis)
  extends Actor {

  var id = self.path.name
  var serviceTime = initialServiceTime

  override def receive: Receive = {

    case init: SetService => {
      id = init.id
      serviceTime = init.serviceTime
      Thread.sleep(1000)
    }

    case msg: PerformanceRoutingMessage => {
      Thread.sleep(serviceTime.toMillis)
      pipe ! msg.copy(
        license = ImageProcessing.getLicense(msg.photo),
        processedBy = Some(id)
      )
    }

  }

}


class RedirectActor(pipe: ActorRef) extends Actor {

  override def receive: Receive = {
    case msg : AnyRef => {
      pipe ! msg
    }
  }

}

class SpeedRouterLogic(minSpeed: Int, normalFlowPath: String, cleanUpPath: String) extends RoutingLogic {

  override def select(message: Any, routees: IndexedSeq[Routee]): Routee = {

    message match {

      case msg: Photo => {
        if (msg.speed > minSpeed)
          findRoutee(routees,normalFlowPath)
        else
          findRoutee(routees,cleanUpPath)
      }

    }

  }

  def findRoutee(routees: IndexedSeq[Routee], path:String) : Routee = {
    val routeeList = routees.flatMap {
      case routee : ActorRefRoutee => routees
      case SeveralRoutees(routeeSeq) => routeeSeq
    }
    val search = routeeList.find {
      case routee: ActorRefRoutee => routee.ref.path.toString.endsWith(path)}
    search.getOrElse(NoRoutee)
  }

}

case class SpeedRouterPool(minSpeed: Int, normalFlow : Props, cleanUp : Props ) extends Pool {

  override def nrOfInstances(sys: ActorSystem): Int = 1

  override def resizer: Option[Resizer] = None

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy()(SupervisorStrategy.defaultDecider)

  override def routerDispatcher: String = Dispatchers.DefaultDispatcherId

  override def createRouter(system: ActorSystem): Router = {
    val normal = system.actorOf(normalFlow,"normalFlow")
    val clean = system.actorOf(cleanUp,"cleanup")
    val routee = immutable.IndexedSeq[Routee](ActorRefRoutee(normal),ActorRefRoutee(clean))
    Router(new SpeedRouterLogic(minSpeed,"normalFlow","cleanup"),routee)
  }

}


case class RouteStateOn()

case class RouteStateOff()

class SwitchRouter(normalFlow : ActorRef, cleanUp: ActorRef) extends Actor with ActorLogging {

  def on : Receive = {
    case RouteStateOn => log.warning("Receive on while already in on state")
    case RouteStateOff => context.become(off)
    case msg : AnyRef => normalFlow ! msg
  }

  def off : Receive = {
    case RouteStateOn => context.become(on)
    case RouteStateOff => log.warning("Receive off while already in off state")
    case msg : AnyRef => cleanUp ! msg
  }

  override def receive: Receive = {
    case msg : AnyRef => off(msg)
  }

}


class SwitchRouter2(normalFlow : ActorRef, cleanUp: ActorRef)
  extends Actor with ActorLogging {

  def on : Receive = {
    case RouteStateOn => log.warning("Received on while on")
    case RouteStateOff => context.unbecome()
    case msg : Any => normalFlow ! msg
  }

  def off : Receive = {
    case RouteStateOn => context.become(on)
    case RouteStateOff => log.warning("Received an off request while off")
    case msg : Any => cleanUp ! msg
  }


  override def receive: Receive = {
    case msg : AnyRef => off(msg)
  }

}



