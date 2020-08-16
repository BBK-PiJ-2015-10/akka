package aia.stream

import java.nio.file.{Files,FileSystems}

import scala.concurrent.Future
import scala.util.{Failure,Success}

import akka.NotUsed
import akka.actor.{ActorSystem}
import akka.event.Logging

import akka.stream.{ActorMaterializer,ActorMaterializerSettings,Supervision}

import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding


import com.typesafe.config.{ConfigFactory}

object ContentNetLogApp extends App{

  val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  val logsDir = {
    val dir = config.getString("log-stream-processor.logs-dir")
    Files.createDirectories(FileSystems.getDefault.getPath(dir))
  }

  val maxLine = config.getInt("log-stream-processor.max-line")
  val maxJsObject = config.getInt("log-stream-processor.max-json-object")

  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher

  val decider : Supervision.Decider = {
    case _ : LogStreamProcessor.LogParseException => Supervision.Stop
    case _                                        => Supervision.Stop
  }

  implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system)
      .withSupervisionStrategy(decider)
  )

  val log = Logging(system.eventStream,"content-net-logs")

  val api = new ContentNetLogsApi(logsDir,maxLine,maxJsObject).routes

  val bindingFuture: Future[ServerBinding] =
    Http().bindAndHandle(api,host,port)


  bindingFuture.map(serverBinding =>
    log.info(s"Bound to ${serverBinding.localAddress}")
  ).onComplete({
    case Failure(ex)=> {log.error(ex, "Failed to bind {}:{}",host,port)
    system.terminate()}
    case Success(_) => log.info("Successfully bound")
  })


}
