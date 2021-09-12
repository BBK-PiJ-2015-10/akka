package streams.documentation.examples.factorial

import akka.actor.ActorSystem
import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source}
import akka.util.ByteString
import akka.{Done, NotUsed, stream}

import java.nio.file.Paths
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object FactorialApp extends App {

  implicit val system: ActorSystem = ActorSystem("QuickStart")
  implicit val ec = system.dispatcher

  val source: Source[Int, NotUsed] = Source(1 to 100)
  //val done: Future[Done] = source.runForeach(i => println(i))

  println("You better work monkey")

  //done
  //.onComplete(_ => system.terminate())

  val factorials = source.scan(BigInt(1))((acc, next) => acc * next)

  //val done2: Future[IOResult] = factorials
    //.map(num => ByteString(s"$num\n"))
    //.runWith(FileIO.toPath(Paths.get("factorials.txt")))

  //done2
    //.onComplete({
     // println("done fuckers")
     // _ => system.terminate()
    //}
    //)

  //def lineSink(filename: String): Sink[String,Future[IOResult]] =
    //Flow[String]
      //.map(s => ByteString(s + "\n"))
      //.toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

  //val done3 : Future[IOResult] =
    // factorials.map(_.toString)
    //.runWith(lineSink("factorials2.txt"))

  //done3
    //.onComplete({
      //println("Done Donkeys")
      //_ => system.terminate()
    //})

  val done4 : Future[Done] = factorials
    .zipWith(Source(0 to 100))((num,idx) => s"$idx! = $num")
    .throttle(1,1.second)
    .runForeach(println)

  done4.onComplete({
    println("Done Donkeys")
    _ => system.terminate()
  })

}
