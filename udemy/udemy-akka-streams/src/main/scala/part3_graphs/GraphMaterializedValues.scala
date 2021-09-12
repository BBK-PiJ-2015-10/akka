package part3_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, FlowShape, SinkShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Sink, Source}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object GraphMaterializedValues extends App {

  implicit val actorSystem = ActorSystem("GraphMaterializedValues")
  implicit val materializer = ActorMaterializer

  val wordSource = Source(List("Akka", "is", "awesome", "rock", "the", "jvm"))
  val printer = Sink.foreach[String](println)
  val counter = Sink.fold[Int, String](0)((count, _) => count + 1)

  /*
  A composite component (sink):
    - prints out all strings which are lowercase
    - COUNTS the strings that are short (<5 chars)
   */
  val complexWordSink = Sink.fromGraph(
    GraphDSL.create(printer,counter)((printerMatValue,counterMatValue) => counterMatValue) { implicit builder =>
      (printerShape, counterShape) =>
        import GraphDSL.Implicits._

        //step2 - shapes
        val broadcast = builder.add(Broadcast[String](2))
        val lowerCaseFilter = builder.add(Flow[String].filter(word => word == word.toLowerCase))
        val shortStringFilter = builder.add(Flow[String].filter(_.length < 5))

        //step3 - connections
        broadcast ~> lowerCaseFilter ~> printerShape
        broadcast ~> shortStringFilter ~> counterShape

        //step4 - the shape
        SinkShape(broadcast.in)
    }
  )

  /*
  val shortStringsCoungFuture = wordSource.toMat(complexWordSink)(Keep.right).run()

  import actorSystem.dispatcher

  shortStringsCoungFuture.onComplete {
    case Success(count) => println(s"The total number of short strings is : $count")
    case Failure(exception) => println(s"The count of short strings failed is: $exception ")
  }
  */


  def enhanceFlow[A, B](flow: Flow[A, B, _]): Flow[A, B, Future[Int]] = {
    val counterSink = Sink.fold[Int, B](0)((count, _) => count + 1)
    Flow.fromGraph(
      GraphDSL.create(counterSink) { implicit builder =>
        counterSinkShape =>
          import GraphDSL.Implicits._

          val broadcast = builder.add(Broadcast[B](2))
          val originalFlowShape = builder.add(flow)

          originalFlowShape ~> broadcast ~> counterSinkShape
          FlowShape(originalFlowShape.in, broadcast.out(1))
      }
    )
  }

  val simpleSource = Source(1 to 42)
  val simpleFlow = Flow[Int].map(x => x)
  val simpleSink = Sink.ignore

  import actorSystem.dispatcher

  val enhancedFlowCountFuture = simpleSource.viaMat(enhanceFlow(simpleFlow))(Keep.right)
    .toMat(simpleSink)(Keep.left).run()

  enhancedFlowCountFuture.onComplete{
    case Success(value) => println(s"The total count is $value")
    case Failure(e) => println(s"Something went off $e")
  }

}
