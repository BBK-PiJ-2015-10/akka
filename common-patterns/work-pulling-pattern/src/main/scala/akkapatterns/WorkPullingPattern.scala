package akkapatterns

import akka.actor.{ActorRef}

object WorkPullingPattern {

  sealed trait Message

  trait Epic[T] extends Iterable[T]

  case object GimmeWork extends Message
  case object CurrentlyBusy extends Message
  case object WorkAvailable extends Message
  case class RegisterWorker(worker: ActorRef) extends Message
  case class Work[T](work: T) extends Message


}


