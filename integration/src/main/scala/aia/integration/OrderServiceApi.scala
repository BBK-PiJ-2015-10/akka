package aia.integration

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import akka.actor.{ActorRef,ActorSystem}
import akka.pattern.ask
import akka.util.Timeout

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.model.StatusCodes.{NotFound,BadRequest}
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.Directives._
//import akka.http.scaladsl.server.Directives.{get,post,entity,path,as,pathPrefix,onSuccess,complete}
import akka.http.scaladsl.server.PathMatchers.{IntNumber}

import scala.xml.{Elem,XML,NodeSeq}

class OrderServiceApi(system : ActorSystem, timeout: Timeout, val processOrders: ActorRef)
   extends OrderService {
      override implicit def requestTimeout: Timeout = timeout
      override implicit def executionContext: ExecutionContext = system.dispatcher

}

trait OrderService {
  val processOrders: ActorRef

  implicit def executionContext: ExecutionContext

  implicit def requestTimeout: Timeout

  val routes = getOrder ~ postOrder


  def getOrder = get {
    pathPrefix("orders" / IntNumber){ id =>
      onSuccess(processOrders.ask(OrderId(id))){
        case result : TrackingOrder => {
          complete(
            <statusResponse>
              <id>{result.id}</id>
              <status>{result.status}</status>
            </statusResponse>)
        }
        case result : NoSuchOrder => complete(NotFound)
      }

    }
  }

  def postOrder = post {
    path("orders") {
      entity(as[NodeSeq]){ xml =>
        val order = toOrder(xml)
        onSuccess(processOrders.ask(order)){
          case result : TrackingOrder =>
            complete(
              <confirm>
                <id>{result.id}</id>
                <status>{result.status}</status>
              </confirm>
            )
          case result => complete(BadRequest)
        }

      }
    }
  }

  def toOrder(xml: NodeSeq): Order = {
    val order = xml \\ "order"
    val customerId = (order \\ "customerId").text
    val productId = (order \\ "productId").text
    val number = (order \\ "number").text.toInt
    new Order(customerId,productId,number)
  }

}




