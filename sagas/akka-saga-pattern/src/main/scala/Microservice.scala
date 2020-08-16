

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow

import scala.concurrent.Future


object Microservice {

  def bind(handler: Flow[HttpRequest,HttpResponse,Any], interface: String = "localhost", port: Int = 80)
          (implicit system: ActorSystem, materializer: ActorMaterializer) : Future[ServerBinding] = {
    Http().bindAndHandle(handler,interface,port)
  }

}
