package nl.jgordijn.servicediscovery.http

import akka.actor.ActorRef
import akka.http.scaladsl.server.{HttpApp, Route}

import scala.concurrent.ExecutionContext

class WebServer(services: ActorRef)(implicit executionContext: ExecutionContext) extends HttpApp{

  override def routes(): Route = new Api(services).route

}
