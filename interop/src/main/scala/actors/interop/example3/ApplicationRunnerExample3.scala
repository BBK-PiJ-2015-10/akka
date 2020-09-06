package actors.interop.example3

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.{Materializer, OverflowStrategy, QueueOfferResult}
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.concurrent.duration.FiniteDuration

//Exammple to emit from an Actor to a Stream
object ApplicationRunnerExample3 extends App{


  implicit val system = ActorSystem("example3")
  implicit val materializer = Materializer(system)


  val butterSize = 10
  val elementsToProcess = 5

  val queue = Source
    .queue[Int](butterSize,OverflowStrategy.backpressure)
    .throttle(elementsToProcess,FiniteDuration.apply(3,TimeUnit.SECONDS))
    .map(x => x * x)
    .toMat(Sink.foreach(x => println(s"completed $x")))(Keep.left)
    .run()

  implicit val ec = system.getDispatcher
  val source = Source(1 to 10)
  source
    .mapAsync(1)(x =>{
      queue.offer(x).map {
        case QueueOfferResult.Enqueued => println(s"enqueued $x")
        case QueueOfferResult.Dropped => println(s"dropped $x")
        case QueueOfferResult.Failure(ex) => println(s"Offer failed ${ex.getMessage}")
        case QueueOfferResult.QueueClosed => println("Source Queue closed")
      }
    } ).runWith(Sink.ignore)







}
