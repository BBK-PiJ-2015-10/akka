package part2_primer

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import scala.language.postfixOps

object BackPressureBasics extends App{

  implicit val actorSystem = ActorSystem("BackpressureBasics")
  implicit val materialer = ActorMaterializer

  val fastSource = Source(1 to 1000)
  val slowSink = Sink.foreach[Int] { x =>
    Thread.sleep(1000)
    println(s"Sink: $x")
  }

  //backpressure
  //fastSource.async.to(slowSink).run()

  val simpleFlow = Flow[Int].map { x =>
    println(s"Incoming $x")
    x + 1
  }

  //default buffer 16 elements
  //fastSource.async.via(simpleFlow).async.to(slowSink).run()

  /*
  - try to slow down if possible
  - buffer elements until there is more demand
  - drop down elements
  - tear down or kill
   */

  //drop oldest elements on the buffer to give room for the new one
  val bufferedFlow = simpleFlow.buffer(10,overflowStrategy = OverflowStrategy.dropHead)
  fastSource.async
    .via(bufferedFlow).async
    .to(slowSink)
    //.run()

  /*
  Overflow strategies:
    - drop head: oldest
    - drop tail: newest
    - drop new: exact element to be added
    - drop the entire buffere
    - backpressure signal
    - fail
   */

  import scala.concurrent.duration._

  fastSource.throttle(2, 1 second).runWith(Sink.foreach(println))


}
