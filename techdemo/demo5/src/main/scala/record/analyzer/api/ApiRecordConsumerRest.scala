package record.analyzer.api

import akka.actor.{ActorLogging,Props}
import akka.camel.{CamelMessage, Consumer}
import org.apache.camel.Exchange
import record.analyzer.state.{Coordinator, FetchRecords}

import util.{Success}


object ApiRecordConsumerRest {
  def props(sourcesUrl: Set[String] , sinkUrl: String) = Props(new ApiRecordConsumerRest(sourcesUrl,sinkUrl))
}

class ApiRecordConsumerRest(sourcesUrl: Set[String], sinkUrl: String) extends Consumer with ActorLogging {

  override def endpointUri: String = "jetty:http://localhost:8181/records"

  val coordinator = context.actorOf(Coordinator.props(sourcesUrl, sinkUrl), "coordinator")

  override def receive: Receive = {
    case msg: CamelMessage => {
      try {
        val action = msg.headerAs[String](Exchange.HTTP_METHOD)
        action match {
          case Success("GET") => {
            coordinator ! FetchRecords
            val headers = Map[String, Any](
              Exchange.CONTENT_TYPE -> "application/json",
              Exchange.HTTP_RESPONSE_CODE -> 200
            )
            sender() ! CamelMessage("started", headers);
          }
        }
      } catch {
        case _: Exception =>
          val headers = Map[String, Any](
            Exchange.CONTENT_TYPE -> "application/json",
            Exchange.HTTP_RESPONSE_CODE -> 500
          )
          sender() ! CamelMessage("bad", headers)
      }
    }

  }

}
