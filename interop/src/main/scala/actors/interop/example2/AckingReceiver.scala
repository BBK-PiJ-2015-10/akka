package actors.interop.example2

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object AckingReceiver {

  case object Ack

  case object StreamInitialized

  case object StreamCompleted

  final case class StreamFailure(ex: Throwable)

  def props(probe: ActorRef, ackWith: Any) = Props(new AckingReceiver(probe,ackWith))

}

class AckingReceiver(probe : ActorRef, ackWith : Any) extends Actor with ActorLogging{
  import AckingReceiver._

  override def receive: Receive = {
    case StreamInitialized =>
      log.info("Stream Initialized!")
      probe ! "Stream Initialized!"
      sender() ! Ack
    case el: String =>
      log.info("Received element: {}",el)
      probe ! el
      sender() ! Ack
    case StreamCompleted =>
      log.info("Stream Completed")
      probe ! "Stream Completed!"
    case StreamFailure(ex) =>
      log.error(ex,"Stream failed!")
  }

}
