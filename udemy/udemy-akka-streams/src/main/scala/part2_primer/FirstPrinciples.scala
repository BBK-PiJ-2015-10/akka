package part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future

object FirstPrinciples  extends App{

  implicit val system = ActorSystem("FirstPrinciples")
  val materializer = ActorMaterializer

  //source
  val source = Source(1 to 10)
  //sinks
  val sink = Sink.foreach[Int](println)
  //graph
  val graph = source.to(sink)
  //graph.run()

  //flows to transform elements
  val flow = Flow[Int].map(x => x + 1)
  val sourceWithFlow = source.via(flow);
  val flowWithSiNK = flow.to(sink)

  //sourceWithFlow.to(sink).run()
  //source.to(flowWithSiNK).run()
  //source.via(flow).to(sink).run()

  //null are NOT allows
  //val illegalSource = Source.single[String](null)
  //illegalSource.to(Sink.foreach(println)).run()
  // Use options instead

  //various kind of sources
  val finiteSource = Source.single(1)
  val anotherFiniteSource = Source(List(1,2,3))
  val emptySource = Source.empty[Int]
  //val infiniteSource = Source(Stream.from(1))

  import scala.concurrent.ExecutionContext.Implicits.global
  val futureSource = Source.fromFuture(Future(42))

  //Sinks
  val theMostBoringSink = Sink.ignore
  val foreachSink = Sink.foreach[String](println)
  val headSink = Sink.head[Int]
  val foldSink = Sink.fold[Int,Int](0)((a,b) => a +b)

  //flows - usually map to collection operators
  val mapFlow = Flow[Int].map(x => 2 * x )
  val takeFlow = Flow[Int].take(5) //turns into a finite stream, the closes the stream
  //drop, filter
  //We do not have flatMap

  //source -> flow -> flow -> .. > sink

  val doubleFlowGraph = source.via(mapFlow).via(takeFlow).to(sink)
  //doubleFlowGraph.run()

  // syntactic sugar
  val mapSource = Source(1 to 10).map(x => x *2) // Source(1 to 10).via(Flow[Int].map(x => x *2))

  //runs streams directly
  //mapSource.runForeach(println)

  //OPERATORS = components


  val names = List("yass","alexander","dog","cat","oter","montealegre","coa","cortez")
  val nameSource = Source(names)
  val longNameFlow = Flow[String].filter(name => name.length > 5)
  val limitFlow = Flow[String].take(2)
  val nameSink = Sink.foreach[String](println)

  //nameSource.via(longNameFlow).via(limitFlow).to(nameSink).run()

  Source(List("yass","alexander","dog","cat","oter","montealegre","dog","papapero"))
    .filter(_.length > 5)
    .take(2)
    .runForeach(println)

}
