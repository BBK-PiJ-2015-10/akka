package nl.jgordijn.servicediscovery

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.server.HttpApp
import akka.http.scaladsl.settings.ServerSettings
import akka.event.Logging
import nl.jgordijn.servicediscovery.http.{Api, WebServer}

import scala.concurrent.ExecutionContext

// sbt -Dakka_port=2552 -Dhttp_port=8181 run
// sbt -Dakka_port=2553 -Dhttp_port=8282 run

object Main extends App{

  implicit val system = ActorSystem("servicediscovery")

  import system.dispatcher

  val log = Logging(system.eventStream,"myLogger")
  val hostname = "localhost"
  val port = system.settings.config.getInt("port")

  log.info(s"Will start the Application on hostName {} and port {}",hostname,port)

  val services = system.actorOf(Props(new ServiceDiscoveryActor),"services")


  new WebServer(services)
    .startServer(hostname,port,ServerSettings(system),system)


}



