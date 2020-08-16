package aia.stream

import java.nio.file.{FileSystems, Files}

import akka.actor.ActorSystem

import scala.concurrent.Future
import scala.concurrent.duration._

import scala.util.{Success,Failure}

import akka.event.Logging

import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}

import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.Http

import com.typesafe.config.{Config, ConfigFactory}

object LogStreamProcessorApp extends  App {


  val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  val logsDir = {
    val dir = config.getString("log-stream-processor.logs-dir")
    Files.createDirectories(FileSystems.getDefault.getPath(dir))
  }

  val notificationsDir = {
    val dir = config.getString("log-stream-processor.notifications-dir")
    Files.createDirectories(FileSystems.getDefault.getPath(dir))
  }

  val metricsDir = {
    val dir = config.getString("log-stream-processor.metrics-dir")
    Files.createDirectories(FileSystems.getDefault.getPath(dir))
  }

  val maxLine = config.getInt("log-stream-processor.max-line")

  val maxJsonObject = config.getInt("log-stream-processor.max-json-object")

  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher

  val decider : Supervision.Decider = {
    case _: LogStreamProcessor.LogParseException => Supervision.Resume
    case _                                     => Supervision.Stop
  }

  implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings.create(system).withSupervisionStrategy(decider)
  )

  val api = new LogStreamProcessorApi(logsDir,notificationsDir,metricsDir,maxLine,maxJsonObject).routes

  val bindingFuture : Future[ServerBinding] = Http().bindAndHandle(api,host,port)

  val log = Logging(system.eventStream,"processor")

  bindingFuture.
    map{ serverBinding => log.info(s"Bound to ${serverBinding.localAddress}")}
    .onComplete({
      case Success(_) => log.info("Bound for successfully bound")
      case Failure(e) => {
        log.error(e,"Failed to bind {}:{}",host,port)
        system.terminate()  }
    })

}
