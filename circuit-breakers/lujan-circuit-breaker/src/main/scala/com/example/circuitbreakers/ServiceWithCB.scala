package com.example.circuitbreakers

import akka.actor.{Actor,ActorLogging,Props}
import akka.pattern.{CircuitBreaker,pipe}

import scala.concurrent.duration._
import scala.concurrent.Future

import Service._

object ServiceWithCB {

  def props(): Props = Props(new ServiceWithCB)

}

class ServiceWithCB extends Actor with ActorLogging with Service {

  import context.dispatcher

  val breaker : CircuitBreaker =
    new CircuitBreaker(
      context.system.scheduler,
      maxFailures = 1,
      callTimeout = 2 seconds,
      resetTimeout = 10 seconds
    ).onOpen(notifyMe("Open"))
    .onClose(notifyMe("Closed"))
    .onHalfOpen(notifyMe("Half Open"))

  override def receive: Receive = {
    case Request =>
      breaker.withCircuitBreaker(Future(callWebService())) pipeTo sender()
  }

  private def notifyMe(state: String): Unit =
    log.warning(s"My CircuitBreaker is no $state")

}
