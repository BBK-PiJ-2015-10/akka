package aia.integration

import akka.actor.{Actor, ActorRef, FSM}
import akka.camel.{CamelMessage, Consumer}
import akka.pattern.ask
import akka.util.Timeout
import org.apache.camel.Exchange

import xml.XML
import util.{Failure, Success}
import collection.mutable
import concurrent.duration._
import concurrent.ExecutionContext

case class TrackingOrder(id: Long, status: String, order: Order)
case class OrderId(id: Long)
case class NoSuchOrder(id: Long)

class ProcessOrders extends Actor {

  val orderList = new mutable.HashMap[Long,TrackingOrder]()
  var lastOrderId = 0L

  override def receive: Receive = {
    case order : Order => {
      lastOrderId +=1
      val newOrder = new TrackingOrder(lastOrderId,"received",order)
      orderList += lastOrderId -> newOrder
      sender() ! newOrder
    }
    case order: OrderId => {
      orderList.get(order.id) match {
        case Some(intOrder) => {
          sender() ! intOrder.copy(status = "processing")
        }
        case None => sender() ! NoSuchOrder(order.id)
      }
    }
    case "reset" => {
      lastOrderId = 0.longValue()
      orderList.clear()
    }

  }
}


class OrderConsumerRest(uri: String, next: ActorRef)
  extends Consumer {

  override def endpointUri: String = uri

  override def receive: Receive = {
    case msg : CamelMessage => {
      try {
        val action = msg.headerAs[String](Exchange.HTTP_METHOD)
        action match {
          case Success("POST") => {
            processOrder(msg.bodyAs[String])
          }
          case Success("GET") => {
            msg.headerAs[String]("id") match {
              case Success(id) =>processStatus(id)
              case other =>
                sender() ! createErrorMessage(400,"ID not set")
            }
          }
          case Success(act) => {
            sender() ! createErrorMessage(400,"Unsupported action %s".format(act))
          }

          case Failure(_) => {
            sender() ! createErrorMessage(400,"HTTP_METHOD not supplied")
          }
        }
      } catch{
        case ex: Exception =>
          sender() ! createErrorMessage(500,ex.getMessage)
      }

    }
  }

  def createErrorMessage(responseCode: Int, body: String): CamelMessage = {
    val headers = Map[String,Any](Exchange.HTTP_RESPONSE_CODE->responseCode)
    CamelMessage(body,headers)
  }

  def processOrder(content: String) : Unit = {
    implicit val timeOut : Timeout = 1.seconds
    implicit val executionContext = context.system.dispatcher

    val order = createOrder(content)
    val askFuture  = next ? order
    val sendResultTo = sender

    val headers = Map[String,Any](
      Exchange.CONTENT_TYPE -> "text/xml",
      Exchange.HTTP_RESPONSE_CODE -> 200
    )

    askFuture.onComplete {
      case Success(result: TrackingOrder) => {
        val response =
          <confirm>
             <id>{result.id}</id>
             <status>{result.status}</status>
          </confirm>
        sendResultTo ! CamelMessage(response.toString(),headers)
      }
      case Success(result) => {
        val response =
          <confirm>
             <status>ID is unknown {result.toString}</status>
          </confirm>
        sendResultTo ! CamelMessage(response.toString(),headers)
      }
      case Failure(exception) => {
         val response =
           <confirm>
             <status>System Failure {exception.getMessage}</status>
           </confirm>
        sendResultTo ! CamelMessage(response.toString(),headers)
      }
    }

  }

  def processStatus(id: String): Unit = {

    implicit val timeOut : Timeout = 1.seconds
    implicit val executionContext = context.system.dispatcher

    val order = OrderId(id.toLong)
    val askFuture = next ? order
    val sendResultTo = sender

    val headers = Map[String,Any](
      Exchange.CONTENT_TYPE -> "text/xml",
      Exchange.HTTP_RESPONSE_CODE -> "200"
    )

    askFuture.onComplete{
      case Success(result : TrackingOrder) => {
        val response =
          <statusResponse>
            <id>{result.id}</id>
            <status>{result.status}</status>
          </statusResponse>
        sendResultTo ! CamelMessage(response.toString(),headers)
      }
      case Success(result : NoSuchOrder) => {
        val response =
          <statusResponse>
            <id>{result.id}</id>
            <status>ID is unknown</status>
          </statusResponse>
        sendResultTo ! CamelMessage(response.toString(),headers)
      }
      case Success(result) => {
        val response =
          <statusResponse>
            <status>Response is unknown {result.toString}</status>
          </statusResponse>
        sendResultTo ! CamelMessage(response.toString(),headers)
      }
      case Failure(ex)=> {
        val response =
          <statusResponse>
            <status>System Failure {ex.getMessage}</status>
          </statusResponse>
        sendResultTo ! CamelMessage(response.toString(),headers)
      }
    }

  }


  def createOrder(content: String): Order = {
    val xml = XML.loadString(content)
    val order = xml \\ "order"
    val customerId = (order \\ "customerId").text
    val productId = (order \\ "productId").text
    val number = (order \\ "number").text.toInt
    new Order(customerId,productId,number)
  }


}





