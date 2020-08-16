package aia.integration

import scala.concurrent.duration._

import scala.xml.NodeSeq
import akka.actor.Props
import akka.util.Timeout

import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._

import akka.http.scaladsl.testkit.ScalatestRouteTest

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class OrderServiceTest extends AnyWordSpec
  with Matchers
  with OrderService
  with ScalatestRouteTest {

  implicit val executionContext = system.dispatcher
  implicit val requestTimeout = Timeout(1.seconds)

  val processOrders = system.actorOf(
    Props(new ProcessOrders),"orders"
  )

  "The order service" should {

    "return not found if the order cannot be found" in {

      Get("/orders/1") ~> routes ~> check {
        status mustBe(StatusCodes.NotFound)
      }

    }

    "return a tracking order for an order that was posted" in {

      val xmlOrder =
        <order><customerId>customer1</customerId>
          <productId>Akka in action</productId>
          <number>10</number>
        </order>

      Post("/orders", xmlOrder) ~> routes ~> check {
        status mustBe  (StatusCodes.OK)
        val xml = responseAs[NodeSeq]
        val id = (xml \\ "id").text.toInt
        val orderStatus = (xml \\ "status").text
        id mustBe  1
        orderStatus mustBe  "received"
      }
      Get("/orders/1") ~> routes ~> check {
        status mustBe  StatusCodes.OK
        val xml = responseAs[NodeSeq]
        val id = (xml \\ "id").text.toInt
        val orderStatus = (xml \\ "status").text
        id mustBe  1
        orderStatus mustBe  "processing"
      }

    }

  }


}
