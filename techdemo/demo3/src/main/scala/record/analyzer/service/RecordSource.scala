package record.analyzer.service

import akka.actor.{ActorLogging, Props}
import akka.camel.{CamelMessage, Producer}
import org.apache.camel.Exchange
import record.analyzer.model.InvalidRecord

object RecordSource {
  def props(source: String) : Props = Props(new RecordSource(source))
}

class RecordSource(source : String) extends Producer with ActorLogging with RecordParser {

  override def endpointUri: String = "http4:"+source

  override def oneway: Boolean = false

  override protected def transformOutgoingMessage(message: Any): Any = {
    message match {
      case _: Any => CamelMessage.apply("",Map(Exchange.HTTP_METHOD ->"GET"))
    }
  }

  override protected def transformResponse(message: Any): Any = {
    try {
      message match {
        case message: CamelMessage => {
          val headers = message.headers
          val body = message.bodyAs[String]
          val record = parse(headers, body)
          log.info(s"Consumed from source ${source} record ${record}")
          record
        }
        case _ => InvalidRecord(message.toString)
      }
    } catch {
      case _ : Exception => InvalidRecord(message.toString)
    }
  }

}
