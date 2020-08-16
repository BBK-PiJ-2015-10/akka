package aia.faulttolerance

import akka.actor.{ActorRef,Actor,ActorLogging,Terminated}

object DBStrategy2 {

  class DBWatcher(dbWriter: ActorRef) extends Actor with ActorLogging {

    context.watch(dbWriter)

    override def receive: Receive = {
      case Terminated(actorRef) => log.warning("Just watched Actor {} terminated",actorRef)
    }

  }

}

