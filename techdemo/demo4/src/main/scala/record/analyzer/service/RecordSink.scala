package record.analyzer.service

import akka.actor.{ActorLogging, Props}
import akka.camel.{CamelMessage, Producer}
import record.analyzer.model.{ProcessedRecord, SubmittedRecord}
import net.liftweb.json.{Serialization}

object RecordSink {
  def props(destination: String) : Props = Props(new RecordSink(destination))
}
class RecordSink(destination: String) extends Producer with ActorLogging with RecordParser  {

  override def endpointUri: String = "http4:"+destination

  override def oneway: Boolean = false

  override protected def transformOutgoingMessage(message: Any): Any = {
     message match {
       case processedRecord : ProcessedRecord =>
         val body = Serialization.write(processedRecord)
         body
    }
  }

  override protected def transformResponse(message: Any): Any = {
      message match {
        case message: CamelMessage => {
          val msg = message.bodyAs[String]
          log.info(s"Actor with name ${self.path.name} submitted record to ${destination} with a message of ${msg}")
          SubmittedRecord
        }
      }
  }

}
