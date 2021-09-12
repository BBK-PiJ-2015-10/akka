package part3_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer,FlowShape, SinkShape, SourceShape}
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, Sink, Source}

object OpenGraphs extends App {

  implicit val system = ActorSystem("OpenGraphs")
  implicit val materializer = ActorMaterializer

  /*
  Composite source that concatenates two sources.
  - emits ALL the elements from the first source
  - then ALL the elements from the second source
   */

  val firstSource = Source(1 to 10)
  val secondSource = Source(42 to 1000)

  val sourceGraph = Source.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val concat = builder.add(Concat[Int](2))

      firstSource ~> concat
      secondSource ~> concat

      SourceShape(concat.out)
    }
  )

  //sourceGraph.to(Sink.foreach(println)).run()

  /*
  Complex Sink
   */
  val sink1 = Sink.foreach[Int](x=> println(s"Meaningful thing 1: $x"))
  val sink2 = Sink.foreach[Int](x => println(s"Meaningful thing 2: $x"))

  val sinkGraph = Sink.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[Int](2))

      broadcast ~> sink1
      broadcast ~> sink2

      SinkShape(broadcast.in)

    }
  )

  //firstSource.to(sinkGraph).run()

  /*
  Challenge- complex flow?
  - One that adds 1 to a number
  - One that does number * 10
   */

  val incrementer = Flow[Int].map(_ + 1)
  val multiplier = Flow[Int].map(_ * 10)

  val flowGraph = Flow.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      //step 2 - def aux shapes
      val incrementerShape = builder.add(incrementer)
      val multiplierShape = builder.add(multiplier)
      //step 3 - chain together shapes
      incrementerShape ~> multiplierShape

      FlowShape(incrementerShape.in, multiplierShape.out)
    } //static graph
  ) //component

  firstSource.via(flowGraph).to(Sink.foreach(println)).run()

  /*
  Exercise: flow from a sink and a source
   */

  val mySource = Source[Int](1 to 10)
  val mySink = Sink.foreach[Int](println)

  def fromSinkAndsSource[A, B](sink: Sink[A, _], source: Source[B, _]): Flow[A, B, _] =
    Flow.fromGraph(
      GraphDSL.create() { implicit builder =>
        //step 2 - def aux shapes
        val sourceShape = builder.add(source)
        val sinkShape = builder.add(sink)
        //step 3 - chain together shapes
        FlowShape(sinkShape.in, sourceShape.out)
      } //static graph
    ) //component


  val f = Flow.fromSinkAndSource(Sink.foreach[Int](println),Source(1 to 10))
  val g = Flow.fromSinkAndSourceCoupled(Sink.foreach[Int](println),Source(1 to 10))


}
