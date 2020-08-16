package de.innfactory.svc1


import akka.actor.{ActorSystem}
import akka.http.scaladsl.UseHttp2.Always
import akka.http.scaladsl.{Http, HttpConnectionContext}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.ActorMaterializer
import akka.util.Timeout
import de.innfactory.common.Config
import de.innfactory.svc1.grpc.GreeterServiceHandler

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object Service1 extends App with Config {

  println("Service1 starting")

  implicit val actorSystem = ActorSystem("service1")
  implicit val materializer = ActorMaterializer()
  implicit val ec = actorSystem.dispatcher
  implicit val timeout = Timeout(FiniteDuration(2,SECONDS))

  // Akka Management hosts the HTTP routes used by bootstrap
  AkkaManagement(actorSystem).start()
  // Starting the bootstrap process needs to be done explicitly
  ClusterBootstrap(actorSystem).start()

  val shardedGreeter = actorSystem.actorOf(ShardedGreeter.props,ShardedGreeter.shardName)

  val routes  =
    (get & pathEndOrSingleSlash){
      complete("Service 1 is ok")
    }

  val intraService: HttpRequest => Future[HttpResponse] =
    GreeterServiceHandler(new GreetersServiceImpl(shardedGreeter))


  Http().bindAndHandle(routes,service1Host,service1Port)

  Http().
    bindAndHandleAsync(intraService,service1Host,service1Grpc,connectionContext = HttpConnectionContext(http2 = Always))



}
