package de.innfactory.svc2

import akka.actor.ActorSystem
import akka.discovery.kubernetes.KubernetesApiServiceDiscovery
import akka.discovery.{Discovery, ServiceDiscovery}
import akka.grpc.GrpcClientSettings
import akka.http.javadsl.server
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.remote.WireFormats
import akka.stream.ActorMaterializer
import de.innfactory.common.Config
import de.innfactory.svc1.grpc._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.concurrent.duration._

object Service2 extends App with Config {

  println("Service2 starting")

  implicit val actorSystem = ActorSystem("service2")
  implicit val materializer = ActorMaterializer()
  implicit val ec = actorSystem.dispatcher

  // Akka Management hosts the HTTP routes used by bootstrap
  AkkaManagement(actorSystem).start()

  val discovery = Discovery(actorSystem).loadServiceDiscovery("kubernetes-api")

  println("Try to resolve gRPC endpoint for service 1")
  val lookup = discovery.lookup("service1",FiniteDuration(20,SECONDS))

  val r = Await.result(lookup,FiniteDuration(20,SECONDS))
  val grpcHost = r.addresses.head.address.get.toString.replace("/","")

  println("resolved:")
  println(r)
  println("addresses:")
  println(r.addresses)

  println(s"Resolved gRPC Endpoints >$grpcHost:$service1Grpc<")


  def askService1(name: String) = {
    actorSystem.log.info("Performing request")
    val reply = client.sayHello(HelloRequest(name))
    reply.onComplete {
      case Success(msg) =>
        println(s"Service 1 send reply : $msg")
      case Failure(e) =>
        println(s"Error sayHello to service 1 : $e")
    }
    reply
  }

  val routes =
    (get & pathEndOrSingleSlash) {
      complete("Service 2 is ok")
    } ~ get{
      pathPrefix("greet") {
        path(PathMatchers.Segment) { name =>
          parameters('delay.as[Int] ? 0, 'responseCode.as[Int] ? 200){ (delay, responseCode) =>
            complete({
              Thread.sleep(delay)
              askService1(name).map(m => {
                responseCode -> s"Hello ${m.message} - The actorsystem greeted you ${m.greeted} times with a delay of $delay ms!"
              })
            })
          }
        }
      }
    }

  Http().bindAndHandle(routes,service2Host,service2Port)


  val client : GreeterService = GreeterServiceClient(clientSettings)

  val clientSettings = GrpcClientSettings
    .connectToServiceAt(grpcHost,service1Grpc)
    .withTls(false).withDeadline(FiniteDuration(2,SECONDS)).withUserAgent("service2")


}
