package aia.stream

import java.nio.file.{FileSystems, Files}

import akka.Done

import scala.concurrent.Future
import scala.util.{Failure, Success}
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.Http
import com.typesafe.config.{Config, ConfigFactory}

object FanLogsApp extends App {

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
    case _ : LogStreamProcessor.LogParseException => Supervision.Resume
    case _                                        => Supervision.Stop
  }

  implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system).withSupervisionStrategy(decider)
  )

  val api = new FanLogsApi(logsDir,maxLine,maxJsObject).routes

  val bindingFuture : Future[ServerBinding] = Http().bindAndHandle(api,host,port)

  val log = Logging(system.eventStream,"fan-logs")

  bindingFuture.map{ serverBinding =>
    log.info(s"Bound to ${serverBinding.localAddress}")
  }.onComplete{
    case Failure(ex) => log.error(ex,"Failed to bind {}:{}!",host,port)
    case Success(_) => log.info("All good to go")
  }


}
