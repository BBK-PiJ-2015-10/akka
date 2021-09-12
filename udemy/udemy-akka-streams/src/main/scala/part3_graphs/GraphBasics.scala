package part3_graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, Materializer}
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}
import scala.language.postfixOps

object GraphBasics  extends  App {

  implicit val system = ActorSystem("GraphBasics")
  implicit val materializer = ActorMaterializer

  val input = Source(1 to 1000)
  val incrementer = Flow[Int].map(_ + 1)
  val multiplier = Flow[Int].map(_ * 10)
  val output = Sink.foreach[(Int,Int)](println)

  val graph = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit builder: GraphDSL.Builder[NotUsed] => //builder = MUTABLE data structure
      import GraphDSL.Implicits._ //brings some nice operators

      //step 2 - add the necessary components of the graph
      val broadcast = builder.add(Broadcast[Int](2)) //fan-out operator
      val zip = builder.add(Zip[Int,Int]) //fan-in operator

      //step 3 - tying up the components
      input ~> broadcast
      broadcast.out(0) ~> incrementer ~> zip.in0
      broadcast.out(1) ~> multiplier ~> zip.in1
      zip.out ~> output
      ClosedShape // shape FREEZE the builder's shape
    }  //graph ()
  ) //runnable graph

  //graph.run() // run the graph and materialize

  //Left on minute 15.25

  /*
  exercise-1: Feed a singleSource into multiple sinks (hint: use a broadcast)
   */

  val sink1 = Sink.foreach[Int](x => println(s"cats $x"))
  val sink2 = Sink.foreach[Int](x => println(s"dogs $x"))

  val sourceToTwoSinksGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[Int](2))

      input ~> broadcast ~> sink1 //implicit port-number
               broadcast ~> sink2

      ClosedShape
    }
  )

  //sourceToTwoSinksGraph .run()

  /*
  Exercise 2 balanced
   */

  import scala.concurrent.duration._
  val fastSource = Source(1 to 40).throttle(5,1 second)
  val slowSource = Source(1 to 40).throttle(2,1 second)

  val sink3 = Sink.fold[Int,Int](0)((count, _)=> {
    println(s"Sink 3 number of elements: $count")
    count + 1
  })

  val sink4 = Sink.fold[Int,Int](0)((count, _)=> {
    println(s"Sink 4 number of elements: $count")
    count + 1
  })

  val twoSourceMergeBalanceTwoSinkGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val merge = builder.add(Merge[Int](2))
      val balance = builder.add(Balance[Int](2))
      fastSource ~> merge ~> balance ~> sink3
      slowSource ~> merge;   balance ~> sink4
      ClosedShape
    }
  )

  twoSourceMergeBalanceTwoSinkGraph.run()

  //Fan-out components - broadcast - balance
  //Fan-in components -zip-zipwith - merge -concat


}
