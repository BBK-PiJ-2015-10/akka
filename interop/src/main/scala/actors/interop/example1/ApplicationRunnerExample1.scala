package actors.interop.example1

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout

import scala.concurrent.duration.DurationInt

object ApplicationRunnerExample1 extends App {

  val system = ActorSystem()
  implicit val ec = system.dispatcher

  implicit val askTimeOut = Timeout(5.seconds)
  implicit val materializer = Materializer.createMaterializer(system)

  val words : Source[String,NotUsed] = Source(List("hello","hi"))

  val translator = system.actorOf(Translator.props(),"translator")

  words.ask[String](parallelism = 5)(translator)
    .map(_.toUpperCase())
    .runWith(Sink.ignore)(materializer)

  println("Finished")


}
