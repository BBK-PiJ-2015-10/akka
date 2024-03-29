package part3_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy, UniformFanInShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, MergePreferred, RunnableGraph, Sink, Source, Zip}

object GraphCycles extends App {

  implicit val actorSystem = ActorSystem("GraphCycles")
  implicit val materializer = ActorMaterializer

  val accelerator = GraphDSL.create(){ implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(Merge[Int](2))
    val incrementerShape = builder.add(Flow[Int].map{x =>
      println(s"Accelerating $x")
      x + 1
    })

    sourceShape ~> mergeShape ~> incrementerShape
                   mergeShape <~ incrementerShape

    ClosedShape

  }

  //RunnableGraph.fromGraph(accelerator).run()
  // grpah cycle deadlock!

  /*
  Solution 1: MergePreferred
   */

  val actualAccelerator = GraphDSL.create(){ implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(MergePreferred[Int](1))
    val incrementerShape = builder.add(Flow[Int].map{x =>
      println(s"Accelerating $x")
      x + 1
    })

    sourceShape ~> mergeShape ~> incrementerShape
    mergeShape.preferred <~ incrementerShape

    ClosedShape

  }

  //RunnableGraph.fromGraph(actualAccelerator).run()

  /*
  Solution 2
   */

  val bufferedRepeater = GraphDSL.create(){ implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(Merge[Int](2))
    val incrementerShape = builder.add(Flow[Int].buffer(10,OverflowStrategy.dropHead).map{ x =>
      println(s"Accelerating $x")
      Thread.sleep(1000)
      x
    })

    sourceShape ~> mergeShape ~> incrementerShape
    mergeShape <~ incrementerShape

    ClosedShape

  }

  //RunnableGraph.fromGraph(bufferedRepeater).run()

  /*
  Cycles can create deadlock!
  - add bounds to the number of elements
  - boundedness vs liveness
   */

  /*
  Challenge: Create a fan-in shape
    - Two input which will be fed with EXACTLY ONE number (1 and 1)
    - output will emit an infinite FIBO sequence based off those 2 numbers
    1,2,3,4,8
    Hit: Use ZipWith, cyles, MergePreferred
   */

  val fibonacciGenerator = GraphDSL.create(){ implicit builder =>
    import GraphDSL.Implicits._

    val zip = builder.add(Zip[BigInt,BigInt])
    val mergePreferred = builder.add(MergePreferred[(BigInt,BigInt)](1))
    val fiboLogic = builder.add(Flow[(BigInt,BigInt)].map{ pair =>
      val last = pair._1
      val previous = pair._2
      Thread.sleep(1000)
      (last + previous, last)
    })
    val broadcast = builder.add(Broadcast[(BigInt,BigInt)](2))
    val extractLast = builder.add(Flow[(BigInt,BigInt)].map(_._1))

    zip.out ~> mergePreferred ~> fiboLogic ~> broadcast ~> extractLast
    mergePreferred.preferred <~ broadcast

    UniformFanInShape(extractLast.out,zip.in0,zip.in1)

  }

  val fiboGraph = RunnableGraph.fromGraph(GraphDSL.create(){ implicit builder =>
    import GraphDSL.Implicits._

    val source1 = builder.add(Source.single[BigInt](1))
    val source2 = builder.add(Source.single[BigInt](1))

    val sink = builder.add(Sink.foreach[BigInt](println))
    val fibo = builder.add(fibonacciGenerator)

    source1 ~> fibo.in(0)
    source2 ~> fibo.in(1)

    fibo.out ~> sink

    ClosedShape

  }

  )

  fiboGraph.run()

}
