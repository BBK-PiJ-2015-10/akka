package service

//https://doc.akka.io/docs/akka/current/stream/operators/ActorFlow/ask.html
//Look at images saved

import java.nio.file.{Path, Paths}

import service.common.LogStatus
import akka.{Done, NotUsed, stream}
import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.model.StatusCode
import akka.stream.{IOResult, Materializer, OverflowStrategy}
import akka.stream.scaladsl._
import akka.util.{ByteString, Timeout}

import scala.concurrent.Future
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit

import akka.pattern.ask
import streams.ApplicationStreamRunner.processorResult

import scala.util.{Failure, Success}


case object Consume
case object DoneConsuming

object LogSource {

  def props(source: String): Props = Props(new LogSource(source))

}

class LogSource(filename: String) extends Actor with ActorLogging{
  import LogSource._
  import service.common.LogStatus

  implicit val materializer = Materializer(context.system)

  implicit val ec = context.dispatcher

  implicit  val timeout : Timeout = 5.second

  private val createPath : Path = Paths.get(getClass.getClassLoader.getResource(filename).getPath)


  private def validateIp(line: String): Boolean = {
    val ipRegex = """.*?(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3}).*""".r
    ipRegex.matches(line.split(",")(0))
  }

  private def extractStatus(line: String): LogStatus = {
    line.split(",").toList match {
      case _ :: _ :: _ :: status :: _ => LogStatus(status)
    }
  }

  private val lineDelimeters: Flow[ByteString,ByteString,NotUsed] =
    Framing.delimiter(
      ByteString("\n"),
      maximumFrameLength = 256,
      allowTruncation = true
    )

  val frame: Flow[ByteString,String,NotUsed] =
    Framing.delimiter(ByteString("/n"),269,true)
      .map(_.decodeString("UTF8"))
  val buildStatus: Flow[String,LogStatus,NotUsed] = Flow[String].map(line => extractStatus(line))

  override def receive: Receive = {
    case Consume => {
      val requestor = sender()
      log.info(s"Received ${Consume} from ${requestor.path.name}")
      val result = FileIO
        .fromPath(createPath)
        .via(lineDelimeters)
        .map(item => item.decodeString("UTF8"))
        .filter(validateIp)
        .via(buildStatus)
        .map(status => requestor ! status)
        .run()
        .onComplete(result => requestor ! DoneConsuming)
      }
  }

}
