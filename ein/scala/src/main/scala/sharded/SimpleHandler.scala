package sharded

import akka.actor.{Actor, ActorLogging, Props}

object SimpleHandler {
  def props(maxCapacity: Int) : Props = Props(new SimpleHandler(maxCapacity))
}

class SimpleHandler(maxCapacity: Int) extends Actor with ActorLogging{

  context.system.eventStream.subscribe(self,classOf[Submit])

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
      sender() ! priceUpdates
      log.debug(s"Sent to Reducer ${priceUpdates.size} prices")
      if (!priceUpdates.isEmpty){
        priceUpdates = Set()
      }
    }

  }

}
