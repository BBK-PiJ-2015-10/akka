
import scala.io.StdIn
import scala.util.{Success,Failure}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import org.slf4j.LoggerFactory

object Main extends App {

  implicit val system = ActorSystem("application")
  implicit val materializer = ActorMaterializer()
  implicit val context = system.dispatcher

  val log = LoggerFactory.getLogger("Main")
  val emailRegion = EmailRegionFactory(system)
  val userRegion = UserRegionFactory(system,emailRegion)
  val userRoutes = UserRoutes(userRegion,emailRegion)

  Microservice.bind(userRoutes,"localhost",8080).onComplete {
    case Success(binding) =>
      log.info("Started, press any key to stop")
      StdIn.readLine()
      binding.unbind().onComplete{
        case _ => system.terminate().onComplete{
          case _ => log.info("Stopped")
        }
      }
    case Failure(exception) =>
      log.error("Failed to start",exception)
  }

}
