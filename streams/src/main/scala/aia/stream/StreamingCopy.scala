package aia.stream

import java.nio.file.{Path, Paths}
import java.nio.file.StandardOpenOption
import java.nio.file.StandardOpenOption._

import scala.concurrent.Future
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, IOResult}
import akka.stream.scaladsl.{FileIO, Keep, RunnableGraph, Sink, Source}
import akka.Done
import akka.util.ByteString
import com.typesafe.config.{Config, ConfigFactory}


object StreamingCopy extends App {

  val config = ConfigFactory.load()
  val maxLine = config.getInt("log-stream-processor.max-line")

  if (args.length !=2 ){
    System.err.println("Provide args: input-file output-file")
    System.exit(1)
  }

  val inputFile = FileArg.shellExpanded(args(0))
  val outputFile = FileArg.shellExpanded(args(1))

  val source : Source[ByteString,Future[IOResult]] = FileIO.fromPath(inputFile)
  val sink: Sink[ByteString,Future[IOResult]] = FileIO.toPath(outputFile,Set(CREATE,APPEND,WRITE))

  //val runnableGraph : RunnableGraph[Future[IOResult]] = source.to(sink)

  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  //runnableGraph.run().foreach { result =>
    //println(s"${result.status}, ${result.count} bytes read.")
    //system.terminate()
  //}

  //val graphRight : RunnableGraph[Future[IOResult]] = source.toMat(sink)(Keep.right)

  //graphRight.run().foreach{ result =>
    //println(s"${result.status}, ${result.count} bytes written.")
    //system.terminate()
  //}

  val graphCustom : RunnableGraph[Future[Done]] =
    source.toMat(sink){(l,r) =>
      Future.sequence(List(l,r)).map(_=> Done)
    }

  graphCustom.run().foreach{ result =>
    println(s"${result} alexis")
    system.terminate()
  }


}
