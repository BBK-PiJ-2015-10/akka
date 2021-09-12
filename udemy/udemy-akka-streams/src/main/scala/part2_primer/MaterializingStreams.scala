package part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.util.{Failure, Success}

object MaterializingStreams extends App{

  implicit val system  = ActorSystem("MaterializingStreams")
  implicit val materializer = ActorMaterializer
  import system.dispatcher;

  val simpleGraph = Source(1 to 10).to(Sink.foreach(println))
 //val simpleMaterializedValue = simpleGraph.run();

  val source = Source(1 to 10)
  val sink = Sink.reduce[Int]((a, b) => a + b)
  //val sumFuture = source.runWith(sink)
 // sumFuture.onComplete{
   // case Success(value) => println(s"The sum of all the elements is :$value")
    //case Failure(exception) => println(s"The sum of the elements could not be computed: $exception")
  //}

  //choosing materialized value
  val simpleSource = Source(1 to 10)
  val simpleFlow = Flow[Int].map(x => x +1)
  val simpleSink = Sink.foreach[Int](println)
  val graph = simpleSource.viaMat(simpleFlow)(Keep.right).toMat(simpleSink)(Keep.right)
  //graph.run().onComplete{
    //case Success(_) => println("Stream processing finished.")
    //case Failure(ex) => println(s"Stream processing failed with : $ex")
  //}

  //sugars
  //val sum = Source(1 to 10).runWith(Sink.reduce[Int](_ + _)) //source.to(Sink.reduce)(Keep.right)
 // val sum2 = Source(1 to 10).runReduce(_ + _)

  //backward
  //Sink.foreach[Int](println).runWith(Source.single(42))
  //bothways
  //Flow[Int].map(x => x * 2).runWith(simpleSource,simpleSink)



  val f1 = Source(1 to 10).toMat(Sink.last)(Keep.right)
  val f2 = Source(1 to 10).runWith(Sink.last)
  f1.run().onComplete{
    case Success(value) => println(s"Last value is $value")
    case Failure(ex) => println(s"Did not work due to $ex")
  }

  val sentenceSource = Source(List("hello my friend","what are you doing","hope is going well"))
  val wordCountSink = Sink.fold[Int,String](0)((currentWordsCount, newSentence) => currentWordsCount + newSentence.split(" ").length)
  val g1 = sentenceSource.toMat(wordCountSink)(Keep.right).run()
  val g2 = sentenceSource.runWith(wordCountSink)
  val g3= sentenceSource.runFold(0)((currentWords,newSentence) => currentWords + newSentence.split(" ").length)

  val wordCountFlow = Flow[String].fold[Int](0)((currentWords,newSentence) => currentWords + newSentence.split(" ").length)
  val g4= sentenceSource.via(wordCountFlow).toMat(Sink.head)(Keep.right).run()
  val g5= sentenceSource.viaMat(wordCountFlow)(Keep.left).toMat(Sink.head)(Keep.right).run()
  val g6 = sentenceSource.via(wordCountFlow).runWith(Sink.head)
  val g7 = wordCountFlow.runWith(sentenceSource,Sink.head)._2
  






}
