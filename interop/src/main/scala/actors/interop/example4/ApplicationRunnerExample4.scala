package actors.interop.example4

import akka.actor.ActorSystem
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.javadsl.Sink
import akka.stream.scaladsl.{Keep, Source}

object ApplicationRunnerExample4 extends App{

  implicit val system = ActorSystem("example4")
  implicit val ec = system.getDispatcher
  implicit val mat = Materializer(system)


  val bufferSize = 10

  val ref = Source
    .actorRef[Int](bufferSize,OverflowStrategy.fail)
    .map(x => x * x)
    .toMat(Sink.foreach(x => println(s"completed $x")))(Keep.left)
      .run()

  ref ! 1
  ref ! 2
  ref ! 3
  ref ! akka.actor.Status.Success("done fucker")


}
