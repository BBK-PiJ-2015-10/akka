package aia.integration

import java.io.{BufferedReader, InputStreamReader, OutputStreamWriter}

import akka.testkit.{TestKit, TestProbe}
import akka.actor.{ActorSystem, Props}
import akka.camel.CamelExtension
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.Await
import xml.XML
import java.net.URL

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class CamelRestTest extends TestKit(ActorSystem("CamelRestTest"))
  with AnyWordSpecLike with BeforeAndAfterAll with Matchers
     {

  implicit val timeout: Timeout = 10.seconds
  implicit val executionContext = system.dispatcher

  override protected def afterAll(): Unit = {
    system.terminate()
  }

  "RestConsumer" must {

    "respond when create" in {

      val orderSystem = system.actorOf(Props[ProcessOrders])

      val camelUri =
        "jetty:http://localhost:8181/orderTest"

      val consumer =
        system.actorOf(Props(new OrderConsumerRest(camelUri,orderSystem)))

      val camelExtension  = CamelExtension(system)
      val activated = camelExtension.activationFutureFor(consumer)
      Await.result(activated,5.seconds)

      val xml = <order>
        <customerId>me</customerId>
        <productId>Akka in Action</productId>
        <number>10</number>
      </order>

      val urlConnection = new URL("http://localhost:8181/orderTest")
      val connection = urlConnection.openConnection()
      connection.setDoOutput(true)
      connection.setRequestProperty("Content-type","text/xml; charset=UTF-8")

      val writer = new OutputStreamWriter(connection.getOutputStream)
      writer.write(xml.toString())
      writer.flush()

      //check result

      val reader = new BufferedReader(
        new InputStreamReader(connection.getInputStream)
      )

      val response = new StringBuffer()
      var line = reader.readLine()
      while (line != null){
        response.append(line)
        line = reader.readLine()
      }
      writer.close()
      reader.close()

      connection.getHeaderField(null) must be ("HTTP/1.1 200 OK")

      val responseXML = XML.loadString(response.toString)
      val confirm = responseXML \\ "confirm"
      (confirm \\ "id").text must be ("1")
      (confirm \\ "status").text must be ("received")

      system.stop(consumer)
      system.stop(orderSystem)
      Await.result(camelExtension.deactivationFutureFor(consumer),5.seconds)

    }

  }

}
