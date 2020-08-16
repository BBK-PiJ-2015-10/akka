
import java.util.concurrent.{ThreadLocalRandom, TimeUnit}

import scala.concurrent.duration.FiniteDuration._
import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable}
import akka.cluster.Cluster
import akka.cluster.ddata.{DistributedData, ORSet, ORSetKey, Replicator, SelfUniqueAddress}
import akka.cluster.ddata.Replicator._

import scala.concurrent.duration.FiniteDuration

object DataBot {

  private case object Tick

}


class DataBot extends Actor with ActorLogging {
  import DataBot._

  val replicator : ActorRef = DistributedData(context.system).replicator
  implicit val node = DistributedData(context.system).selfUniqueAddress
  import context.dispatcher

  val tickTask : Cancellable = context.system.scheduler.
    scheduleWithFixedDelay(FiniteDuration(5,TimeUnit.SECONDS),FiniteDuration(5,TimeUnit.SECONDS),self,Tick)

  val DataKey = ORSetKey[String]("key")

  replicator ! Subscribe(DataKey,self)

  override def receive: Receive = {
    case Tick =>
      val s = ThreadLocalRandom.current().nextInt(97,123).toString
      if (ThreadLocalRandom.current().nextBoolean()){
        log.info("Adding: {}",s)
        replicator ! Update(DataKey,ORSet.empty[String],WriteLocal)(_ :+ s)
      } else {
        log.info("Removing : {}", s)
        replicator ! Update(DataKey,ORSet.empty[String],WriteLocal)(_.remove(s))
      }

    case _ : UpdateResponse[_] => // ignore

    case c @Changed(DataKey) =>
      val data =  c.get(DataKey)
      log.info("Current elements: {}",data.elements)

  }

  override def postStop(): Unit = tickTask.cancel()
}
