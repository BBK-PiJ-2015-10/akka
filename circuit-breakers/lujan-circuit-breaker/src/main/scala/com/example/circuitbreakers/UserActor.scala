package com.example.circuitbreakers

import akka.actor.{ActorLogging, ActorRef,Actor,Props}

import scala.concurrent.duration._

import akka.pattern.{ask,pipe,AskTimeoutException}
import akka.util.Timeout
import akka.actor.Status.Failure

import Service._

object UserActor {

  def props(service: ActorRef): Props = Props(new UserActor(service))

}

class UserActor(service: ActorRef) extends Actor with ActorLogging {

  import context.dispatcher

  private val userWaitTimeout = Timeout(3 seconds)

  sendRequest()

  override def receive: Receive = {
    case Response =>
      log.info("Got a quick response, I'm a happy actor")
      sendRequest()
    case Failure(ex: AskTimeoutException) =>
      log.error("Got bored waiting, I am outta here!")
      context.stop(self)
    case Failure(ex: Exception) =>
      log.info("Something must be wrong with the server, let me try again in a few seconds")
      sendRequest(5 seconds)
    case other => log.info(s"Got another message: $other")

  }


  private def sendRequest(delay: FiniteDuration = 1 second) = {
    // Send a message, pipe response to ourselves
    context.system.scheduler.scheduleOnce(delay) {
      service.ask(Request)(userWaitTimeout) pipeTo self
    }
  }


}
