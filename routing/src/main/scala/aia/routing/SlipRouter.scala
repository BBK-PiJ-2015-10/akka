package aia.routing

import akka.actor.{Props,ActorRef,Actor}

import scala.collection.mutable.ListBuffer


object CarOptions extends Enumeration {
  val CAR_COLOR_GRAY, NAVIGATION, PARKING_SENSORS = Value
}

case class Order(options: Seq[CarOptions.Value])

case class Car(color: String = "", hasNavigation: Boolean = false, hasParkingSensors: Boolean = false)

case class RouteSlipMessage(routeSlip: Seq[ActorRef], message: AnyRef)

trait RouteSlip {

  def sendMessageToNextTask(routeSlip: Seq[ActorRef], message: AnyRef): Unit ={
    val nextTask = routeSlip.head
    val newSlip = routeSlip.tail
    if (newSlip.isEmpty){
      nextTask ! message
    } else {
      nextTask ! RouteSlipMessage(routeSlip = newSlip, message = message)
    }
  }

}

class PaintCar(color: String) extends Actor with RouteSlip {
  override def receive: Receive = {
    case RouteSlipMessage(routeSlip, car: Car) => {
      sendMessageToNextTask(routeSlip,car.copy(color = color))
    }
  }
}

class AddNavigation() extends Actor with RouteSlip {
  override def receive: Receive = {
    case RouteSlipMessage(routeSlip, car: Car) => {
      sendMessageToNextTask(routeSlip,car.copy(hasNavigation = true))
    }
  }
}

class AddParkingSensors() extends Actor with RouteSlip {
  override def receive: Receive = {
    case RouteSlipMessage(routeSlip, car: Car) => {
      sendMessageToNextTask(routeSlip,car.copy(hasParkingSensors = true))
    }
  }
}

class SlipRouter(endStep: ActorRef) extends Actor with RouteSlip {

  val paintBlack = context.actorOf(Props(new PaintCar("black")),"paintBlack")

  val paintGray = context.actorOf(Props(new PaintCar("gray")),"paintGray")

  val addNavigation = context.actorOf(Props(new AddNavigation),"navigation")

  val addParkingSensors = context.actorOf(Props(new AddParkingSensors),"parkingSensors")

  override def receive: Receive = {
    case order : Order => {

      val routeSlip = createRouteSlip(order.options)

      sendMessageToNextTask(routeSlip,new Car)

    }
  }

  private def createRouteSlip(options: Seq[CarOptions.Value]) : Seq[ActorRef] = {

    val routeSlip = new ListBuffer[ActorRef]

    if (!options.contains(CarOptions.CAR_COLOR_GRAY)){
      routeSlip += paintBlack
    }
    options.foreach {
      case CarOptions.CAR_COLOR_GRAY => routeSlip += paintGray
      case CarOptions.NAVIGATION => routeSlip += addNavigation
      case CarOptions.PARKING_SENSORS => routeSlip += addParkingSensors
      case other                      =>
    }

    routeSlip +=endStep
    routeSlip.toSeq

  }

}
