package streams

import akka.actor.ActorSystem

import scala.concurrent.Future
import scala.util.{Failure, Success}

object ApplicationStreamRunner extends App {

  implicit val system = ActorSystem("streamExample")
  implicit val dispatcher = system.dispatcher

  val processor = new LogProcessor(("data/weblogbad.csv"))
  val processorResult: Future[Seq[(String, Long)]] = processor.result

  processorResult.onComplete {
    case Success(result) =>
      println("Akka Stream Completed fuckers")
      result.sortBy(_._2).foreach(println)
    case Failure(ex) =>
      println("Something went off", ex)
  }

}
