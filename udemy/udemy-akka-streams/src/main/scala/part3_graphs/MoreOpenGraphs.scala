package part3_graphs

import java.util.Date

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FanOutShape2, UniformFanInShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}

object MoreOpenGraphs extends App {

  implicit val actorSystem = ActorSystem("OpenGraphs")
  implicit val materializer = ActorMaterializer

  /*
     Max3 operator:
     - 3 inputs of type int
     - the maximum of the 3
   */

  val max3StaticGraph = GraphDSL.create(){ implicit builder =>
    import GraphDSL.Implicits._

    //step 2 - define aux shapes
    val max1 = builder.add(ZipWith[Int,Int,Int]((a,b) => Math.max(a,b)))
    val max2 = builder.add(ZipWith[Int,Int,Int]((a,b) => Math.max(a,b)))

    //step 3
    max1.out ~> max2.in0

    //step 4
    UniformFanInShape(max2.out,max1.in0,max1.in1,max2.in1)
  }


  val source1 = Source(1 to 10)
  val source2 = Source((1 to 10).map(_ => 5))
  val source3 = Source((1 to 10).reverse)

  val maxSink = Sink.foreach[Int](x => println(s"Max is $x"))

  val max3RunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit builder =>
      import GraphDSL.Implicits._

      //step 2 - declare shapes
      val max3Shape = builder.add(max3StaticGraph)

      //step 3 - tie
      source1 ~> max3Shape.in(0)
      source2 ~> max3Shape.in(1)
      source3 ~> max3Shape.in(2)
      max3Shape.out ~> maxSink

      ClosedShape
    }
  )

  //max3RunnableGraph.run()

  // same for uniform fanOutShape

  /*
  Non-Uniform shape fan out shape
    Processing bank transactions:
      - Txn suspicious if > 10,000
      Streams components for txns:
       - let the txn go through, output0
       - send only suspicious txnId for further research, output2
   */


  case class Transaction(id: String, source: String, recicipient: String, amount: Int, date: Date)

  val transactionSource = Source(List(
    Transaction("12345","Paul","Jim",100,new Date()),
    Transaction("12346","Thiago","Jim",1000000,new Date()),
    Transaction("12347","Jim","Alex",7000,new Date()),
  ))

  val bankProcessor = Sink.foreach(println)
  val suspiciousAnalysisService = Sink.foreach[String](txnId =>println(s"Suspicious Transaction ID: $txnId" ))

  //step 1
  val suspiciousTxnStaticGraph = GraphDSL.create(){ implicit builder =>
    import GraphDSL.Implicits._

    //step2 - define shapes
    val broadcast = builder.add(Broadcast[Transaction](2))
    val suspiciousTxnFilter = builder.add(Flow[Transaction].filter(txn => txn.amount > 10000))
    val txnIdExtractor = builder.add(Flow[Transaction].map[String](txn => txn.id ))
    //step3 - tie shapes

    broadcast.out(0) ~> suspiciousTxnFilter ~> txnIdExtractor

    //step 4 -
    new FanOutShape2(broadcast.in,broadcast.out(1),txnIdExtractor.out)

  }

  val suspiciousTxnRunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit builder =>
    import GraphDSL.Implicits._

    //step 2
    val suspiciousTxnShape = builder.add(suspiciousTxnStaticGraph)

    //step-3
    transactionSource ~> suspiciousTxnShape.in
    suspiciousTxnShape.out0 ~> bankProcessor
    suspiciousTxnShape.out1 ~> suspiciousAnalysisService

    //step-4
    ClosedShape

  })

  suspiciousTxnRunnableGraph.run()


}
