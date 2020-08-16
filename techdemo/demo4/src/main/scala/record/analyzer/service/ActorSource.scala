package record.analyzer.service

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest,HttpMethods}
import akka.stream.ActorMaterializer
import scala.util.{Failure, Success}

import record.analyzer.model.InvalidRecord

object ActorSource {
  def props(source: String) = Props(new ActorSource(source))
}

class ActorSource(source: String) extends Actor with ActorLogging with RecordParser {

  implicit val materializer = ActorMaterializer()
  implicit val system = context.system
  implicit val ec = context.dispatcher
  def endpointUri: String = "http://"+source

  override def receive: Receive = {
    case _: Any => {
      val requestor = sender()
      val responseFuture = Http().singleRequest(HttpRequest(method = HttpMethods.GET, uri = endpointUri))
      responseFuture
        .onComplete {
          case Success(response) => {
            val headers: Map[String, Any] = Map("Content-Type" -> response.entity.contentType.value)
            val bodyResponseFuture = Unmarshal(response.entity).to[String]
            bodyResponseFuture
              .onComplete {
                case Success(body) => {
                  val record = parse(headers, body)
                  log.info(s"Consumed from source ${source} record ${record}")
                  requestor ! record
                }
                case Failure(e) => {
                  requestor ! InvalidRecord(e.toString)
                }
              }
          }
          case Failure(e) => {
            requestor ! InvalidRecord(e.toString)
          }
        }
    }
  }

}
