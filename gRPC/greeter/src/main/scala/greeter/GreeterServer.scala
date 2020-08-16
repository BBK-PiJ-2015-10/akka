package greeter

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.SystemMaterializer
//import akka.http.scaladsl.{Http, HttpConnectionContext}
import akka.http.scaladsl._
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.ConfigFactory
import greeter._

import scala.concurrent.{Future,ExecutionContext}

//source: https://doc.akka.io/docs/akka-grpc/current/server/walkthrough.html
// packate.service.method
//grpcurl -d '{"name": "foo"}' -plaintext     -import-path src/main/protobuf     -proto greeter.proto     localhost:8080 greeter.GreetingService.ItKeepsReplying
object GreeterServer extends App {

  //val conf = ConfigFactory
    //.parseString("akka.http.server.preview.enable-http2 =  on")
    //.withFallback(ConfigFactory.defaultApplication())

  implicit val system = ActorSystem("HelloWord")
  implicit val mat = SystemMaterializer(system).materializer

  Http().bindAndHandleAsync(
    GreetingServiceHandler(new GreeterServiceImpl),
      interface = "127.0.0.1",
      port = 9090
    )

//}

/*
class GreeterServer(system: ActorSystem) {

  //import example.myapp.helloworld.grpc._

  def run(): Future[Http.ServerBinding] = {

    implicit val sys: ActorSystem = system
    implicit val mat: Materializer = ActorMaterializer()
    implicit val ec: ExecutionContext = sys.dispatcher

    val service: HttpRequest => Future[HttpResponse] =
      GreetingServiceHandler(new GreeterServiceImpl())


    val binding = Http().bindAndHandleAsync(
      service,
      interface = "127.0.0.1",
      port = 8080,
      connectionContext = HttpConnectionContext())

    binding.foreach( binding => println(s"gRPC server bound to: ${binding.localAddress}"))

    binding

  }

  */

}
