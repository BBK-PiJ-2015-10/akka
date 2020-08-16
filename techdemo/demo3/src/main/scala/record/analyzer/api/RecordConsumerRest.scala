package record.analyzer.api

import akka.actor.{ActorLogging, Props}
import akka.camel.{CamelMessage, Consumer}
import org.apache.camel.Exchange
import record.analyzer.state.Coordinator

import util.{Failure, Success}


object RecordConsumerRest {
  def props(uri: String) = Props(new RecordConsumerRest(uri))
}

class RecordConsumerRest(uri: String) extends Consumer with ActorLogging {

  override def endpointUri: String = uri

  override def receive: Receive = {
    case msg: CamelMessage => {
      try {
        val action = msg.headerAs[String](Exchange.HTTP_METHOD)
        action match {
          case Success("GET") => {
            val sourceAUrl = "localhost:7299/source/a"
            val sourceBUrl = "localhost:7299/source/b"
            val sinkAUrl = "localhost:7299/sink/a"

            val sources = Set(sourceAUrl,sourceBUrl)
            context.actorOf(Coordinator.props(sources,sinkAUrl))

            val headers = Map[String,Any](
              Exchange.CONTENT_TYPE -> "application/json",
              Exchange.HTTP_RESPONSE_CODE -> 200
            )
            sender() ! CamelMessage("started",headers);
          }
        }
      } catch {
        case ex: Exception =>
          val headers = Map[String,Any](
            Exchange.CONTENT_TYPE -> "application/json",
            Exchange.HTTP_RESPONSE_CODE -> 500
          )
          sender() ! CamelMessage("bad",headers)
      }
    }

  }

}
