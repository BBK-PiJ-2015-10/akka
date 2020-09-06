package actors.interop.example2

import akka.actor.{Actor, ActorLogging, Props}

object EchoActor {
  def props(): Props = Props(new EchoActor)
}

class EchoActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case msg : Any =>
      log.info("Echo got {}",msg.toString)
  }
}
