import akka.actor.{Actor, ActorLogging, Props}

object LoggerActor {
  def props(): Props = Props(new LoggerActor())
}


class LoggerActor extends Actor with ActorLogging{

  override def receive: Receive = {
    case x => log.info(s"Received ${x}")
  }
}
