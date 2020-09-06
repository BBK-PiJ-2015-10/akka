package actors

//https://doc.akka.io/docs/akka/current/stream/operators/ActorFlow/ask.html
//Look at images saved

import java.nio.file.{Path, Paths}

import actors.common.LogStatus
import akka.{Done, NotUsed}
import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.model.StatusCode
import akka.stream.{IOResult, Materializer}
import akka.stream.scaladsl._
import akka.util.{ByteString, Timeout}

import scala.concurrent.Future
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit

import akka.pattern.ask

object LogSource {

  def props(source: String): Props = Props(new LogSource(source))

  case object Consume

}

class LogSource(filename: String) extends Actor with ActorLogging{
  import LogSource._

  implicit val materializer = Materializer(context.system)

  //implicit val timeout = Timeout.durationToTimeout(FiniteDuration(5,TimeUnit.SECONDS))
  implicit  val timeout : Timeout = 5.second

  val file : Path = Paths.get(getClass.getClassLoader.getResource(filename).getPath)

  val fileSource: Source[ByteString, Future[IOResult]] = FileIO.fromPath(file)

  val frame: Flow[ByteString,String,NotUsed] =
    Framing.delimiter(ByteString("/n"),256,true)
      .map(_.decodeString("UTF8"))

  val buildStatus: Flow[String,LogStatus,NotUsed] = Flow[String].map(line => extractStatus(line))

  val words: Source[String, NotUsed] =
    Source(List("hello", "hi"))

  //val actorFlow : Flow[Status,String,NotUsed] = A


  override def receive: Receive = {
    case Consume => {
      val ref = sender()
      //words.to
      //fileSource
        //.via(frame)
        //.filter(line => validateIP(line))
        //.map(line => extractStatus(line))
        //.ask[String](ref)
        //.runWith(Sink.ignore)
      //responder ? "alexis"
      //source
        //  .via(frame)
          //.filter(line => validateIP(line))
          //.via(buildStatus)
          //.ask[Status](responder)(Done.apply)


    }
  }


  private def validateIP(line: String): Boolean = {
    val ipRegex = """.*?(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3}).*""".r
    ipRegex.matches(line.split(",")(0))
  }

  private def extractStatus(line: String) : LogStatus = {
    line.split(",").toList match {
      case _ :: _ :: _ :: status :: _ => {
        LogStatus(status.trim)
      }
    }
  }




}
