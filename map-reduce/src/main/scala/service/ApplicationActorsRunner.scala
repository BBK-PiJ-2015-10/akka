package service

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import service.common.StartFetching

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.{Failure, Success}


/*
TODO:
 Try different hashing strategies and maybe the size ()
  - https://doc.akka.io/docs/akka/current/routing.html



 */
// Add log to understand why I am not seeing the message
object ApplicationActorsRunner extends App{

  val path = "data/weblog.csv"

  val system = ActorSystem("withActors")

  implicit val ec = system.getDispatcher

  val coordinator = system.actorOf(LogCoordinator.props(), "coordinator")



  implicit  val timeout : Timeout = 5.second

  val result = coordinator ? StartFetching(path)

  result.onComplete{
    case Success(result) =>  println(s"These are the results ${result}")
    case Failure(e) => println(s"Failure of type ${e}")
  }


  while (true){

  }




}
