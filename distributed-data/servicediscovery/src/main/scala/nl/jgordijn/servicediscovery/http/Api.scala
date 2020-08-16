package nl.jgordijn.servicediscovery.http

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import io.circe.generic.auto._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import nl.jgordijn.servicediscovery.ServiceDiscoveryActor

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

//http localhost:8080/services/myservice host=different port=9876
// using httpi

object Api {

  case class Registration(host: String, port: Int)

}

class Api(serviceDiscoveryActor : ActorRef)(implicit executionContext: ExecutionContext) extends FailFastCirceSupport {
  import Api._
  implicit val timeOut : Timeout = 3.seconds


  val route : Route = pathPrefix("services"){
    path(Segment){ name =>
      get {
        onSuccess(serviceDiscoveryActor ? ServiceDiscoveryActor.Get(name)){
          case ServiceDiscoveryActor.Result(s) => complete(s)
        }
      } ~
      post {
        entity(as[Registration]){ r =>
          onSuccess(serviceDiscoveryActor ? ServiceDiscoveryActor.Register(name,r.host,r.port)){ _ =>
            complete(StatusCodes.NoContent)
          }
        }
      }
    }
  }
}
