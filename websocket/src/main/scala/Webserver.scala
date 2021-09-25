import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.http.scaladsl.model.ws.{Message, TextMessage}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.StdIn
import scala.util.{Failure, Success}

//Source : https://medium.com/@nnnsadeh/building-a-reactive-distributed-messaging-server-in-scala-and-akka-with-websockets-c70440c494e3
object Webserver extends App{

  implicit val actorSystem = akka.actor.ActorSystem("messaging-actorsystem")
  implicit val materializer = ActorMaterializer
  implicit val ec = actorSystem.dispatcher

  implicit val spawnSystem = ActorSystem(SpawnProtocol(), "spawn")


  def helloRoute: Route = pathEndOrSingleSlash {
    complete("Welcome to message service")
  }

  def affirmRoute = path("affirm") {
    handleWebSocketMessages(
      Flow[Message].collect {
        case TextMessage.Strict(text) => TextMessage("You said " + text)
      }
    )
  }

  def messageRoute  =
    pathPrefix("message" / Segment) { trainerId =>
        Await.ready(ChatSessionMap.findOrCreate(trainerId).webflow(),Duration.Inf).value.get
        match {
          case Success(value)  => handleWebSocketMessages(value)
          case Failure(exception) =>
            println(exception.getMessage)
            failWith(exception)
        }
    }

  // bind the route using HTTP to the server address and port
  val binding = Http().bindAndHandle(helloRoute ~ affirmRoute ~ messageRoute, "localhost", 8080)


  println("Server running ...")

  StdIn.readLine()
  binding.flatMap(_.unbind()).onComplete(_=> actorSystem.terminate())
  println("Sever is shut down")

}
