package aia.integration

import akka.camel.{Producer,CamelMessage,Consumer}
import akka.actor.ActorRef

import net.liftweb.json.{Serialization,DefaultFormats}
import xml.XML

import scala.concurrent.duration._

case class Order(customerId: String, productId: String, number: Int)

class OrderConsumerJson(uri: String, next: ActorRef)
  extends Consumer {

  override def endpointUri: String = uri

  implicit val formats = DefaultFormats

  override def receive: Receive = {
    case msg: CamelMessage => {
      val content = msg.bodyAs[String]
      val order = Serialization.read[Order](content)
      next ! order
    }
  }

}

class OrderConsumerXml(uri: String, next: ActorRef)
  extends Consumer {
  override def endpointUri: String = uri

  override def receive: Receive = {
    case msg: CamelMessage => {
      val content = msg.bodyAs[String]
      val xml = XML.loadString(content)
      val order = xml \\ "order"
      val customer = (order \\ "customerId").text
      val productId = (order \\ "productId").text
      val number = (order \\ "number").text.toInt
      next ! Order(customer,productId,number)
    }
  }
}




class OrderConfirmConsumerXml(uri: String, next: ActorRef)
   extends Consumer {

  override def endpointUri: String = uri

  override def receive: Receive = {
    case msg: CamelMessage => {
      try {
        val content = msg.bodyAs[String]
        val xml = XML.loadString(content)
        val order = xml \\ "order"
        val customer = (order \\ "customerId").text
        val productId = (order \\ "productId").text
        val number = (order \\ "number").text.toInt
        next ! Order(customer,productId,number)
        sender() ! "<confirm>OK</confirm>"
      } catch {
        case ex : Exception =>
          sender() ! "<confirm>%s</confirm>".format(ex.getMessage)
      }
    }
  }
}

class SimpleProducer(uri: String)
  extends Producer {

  override def endpointUri: String = uri
}


class OrderProducerXml(uri: String)
  extends Producer {

  override def endpointUri: String = uri

  override def oneway: Boolean = true

  override protected def transformOutgoingMessage(msg: Any): Any = {
    msg match {
      case msg : Order => {
        val xml = <order>
                     <customerId>{msg.customerId}</customerId>
                     <productId>{msg.productId}</productId>
                     <number>{msg.number}</number>
                  </order>
        xml.toString().replace("\n","")
      }
      case other => msg
    }

  }
}

class OrderConfirmedProducerXML(uri: String)
   extends Producer {

  override def endpointUri: String = uri

  override def oneway: Boolean = false


  override protected def transformOutgoingMessage(msg: Any): Any = {
    msg match {
      case msg: Order => {
        val xml =
          <order>
            <customerId>{msg.customerId}</customerId>
            <productId>{msg.productId}</productId>
            <number>{msg.number}</number>
          </order>

        xml.toString().replace("\n","") +"\n"
      }
      case other => msg
    }
  }

  override protected def transformResponse(msg: Any): Any = {
    msg match {
      case msg: CamelMessage => {
        try {
          val content = msg.bodyAs[String]
          val xml = XML.loadString(content)
          val res = (xml \\ "confirm").text
          res
        } catch {
          case ex: Exception => "TransformException: %s".format(ex.getMessage)
        }
      }
      case other => msg
    }
  }
}



