package com.example.circuitbreakers

import akka.actor.ActorLogging

import scala.util.Random

import Service._

object Service {
  case object Request
  case object Response
}


trait Service {

  self: ActorLogging =>

  //Max counts and delays
  private val normalDelay = 100
  private val restartDelay = 3200

  protected def callWebService(): Response.type = {

    if (Random.nextDouble() <= 0.9 ){
      Thread.sleep(normalDelay)
    } else {
      log.error("!!Service overloaded !! Restarting !!")
      Thread.sleep(restartDelay)
    }

    Response
  }


}
