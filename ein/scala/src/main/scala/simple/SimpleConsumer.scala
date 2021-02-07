import akka.actor.{Actor, ActorLogging, Props}

object SimpleConsumer {
  def props(): Props = Props(new SimpleConsumer)
}

class SimpleConsumer extends Actor with ActorLogging{
  override def receive: Receive = {
    case priceUpdate: PriceUpdate => {
      log.info(s"Publishing ${priceUpdate}")
    }
  }

}
