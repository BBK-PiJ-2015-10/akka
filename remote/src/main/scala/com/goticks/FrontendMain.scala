package com.goticks

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext

object FrontendMain extends App with Startup {

  val config = ConfigFactory.load("frontend")

  implicit val system = ActorSystem("frontend",config)

  val api = new RestApi {

    val log = Logging(system.eventStream,"frontend")

    implicit val requestTimeout = configuredRequestTimeout(config)
    implicit val executionContext = system.dispatcher

    def createPath(): String = {
      val config = ConfigFactory.load("frontend").getConfig("backend")
      val host = config.getString("host")
      val port = config.getString("port")
      val protocol = config.getString("protocol")
      val systemName = config.getString("system")
      val actorName = config.getString("actor")
      s"$protocol://$systemName@$host:$port/$actorName"

    }

    override def createBoxOffice(): ActorRef  = {
        val path = createPath()
        system.actorOf(Props(new RemoteLookUpProxy(path)),"lookupBoxOffice")
        }



  }
}
