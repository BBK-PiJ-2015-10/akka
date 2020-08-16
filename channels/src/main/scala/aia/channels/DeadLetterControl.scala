package aia.channels

import akka.actor.{Actor}

class EchoActor extends Actor {

  override def receive: Receive = {
    case msg : AnyRef =>
      sender() ! msg
  }

}