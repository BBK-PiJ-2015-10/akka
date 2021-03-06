package com.example.circuitbreakers

import akka.actor.{ActorLogging,Actor,Props}
import Service._

object ServiceWithoutCB {

  def props(): Props = Props(new ServiceWithoutCB)

}

class ServiceWithoutCB extends Actor with ActorLogging with Service {

  override def receive: Receive = {
    case Request => sender() ! callWebService()
  }

}
