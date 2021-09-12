import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object EchoActor {

  def props(): Props = Props(new EchoActor)

}


class EchoActor extends Actor with ActorLogging{
  override def receive: Receive = {
    case price: String => {
      log.info(s"Have received ${price}")
      //destination ! price
      sender() ! "HIT ME AGAIN"
    }
    case prices: List[String] => {
      sender() ! "HIT ME AGAIN"
    }

  }
}
