package simple

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

case class Submit()

object SimpleHandler {
  def props(maxCapacity: Int,consumer: ActorRef) : Props = Props(new SimpleHandler(maxCapacity,consumer))
}

class SimpleHandler(maxCapacity: Int, consumer: ActorRef) extends Actor with ActorLogging{

  var priceUpdates : Set[PriceUpdate]= Set()

  override def receive: Receive = {

    case priceUpdate: PriceUpdate => {
      if (priceUpdates.contains(priceUpdate)){
        log.debug(s"Updating ${priceUpdate}")
        priceUpdates -= priceUpdate
        priceUpdates += priceUpdate
      } else {
        log.debug(s"Loading ${priceUpdate}")
        if (priceUpdates.size < maxCapacity){
          priceUpdates += priceUpdate
        }
      }
    }

    case Submit => {
      log.debug(s"Sending to Reducer ${priceUpdates.size} prices")
      priceUpdates.foreach(price => consumer ! price)
      log.debug(s"Sent to Reducer ${priceUpdates.size} prices")
      if (!priceUpdates.isEmpty){
        priceUpdates = Set()
      }
    }

  }

}
