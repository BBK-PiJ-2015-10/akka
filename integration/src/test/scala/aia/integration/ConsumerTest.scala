package aia.integration

import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.must.Matchers

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import java.io.{BufferedReader, File, InputStreamReader, PrintWriter}
import java.net.{InetSocketAddress, ServerSocket, Socket, SocketAddress}
import javax.jms.{Session,DeliveryMode,Connection}

import collection.JavaConverters._
import org.apache.commons.io.FileUtils
import akka.actor.{ActorSystem, Props}
import akka.camel.{CamelExtension, CamelMessage}

import org.apache.activemq.ActiveMQConnectionFactory
import org.apache.activemq.camel.component.ActiveMQComponent


class ConsumerTest extends TestKit(ActorSystem("ConsumerTest"))
  with AnyWordSpecLike with BeforeAndAfterAll with Matchers
  with ImplicitSender {

  val dir = new File("messages")

  override protected def beforeAll() = {

    if (!dir.exists()){
      dir.mkdir()
    }

    val mqData = new File("activemq-data")
    if (mqData.exists()){
      FileUtils.deleteDirectory(mqData)
    }

  }

  override protected def afterAll() = {
    system.terminate()
    FileUtils.deleteDirectory(dir)
  }

  "Consumer" must {

    "pickup xml files" in {

      val probe = TestProbe()
      val camelUri = "file:messages"
      val consumer = system.actorOf(
        Props(new OrderConsumerXml(camelUri,probe.ref))
      )

      val camelExtension = CamelExtension(system)
      val activated = camelExtension.activationFutureFor(consumer)(timeout = 10.seconds, executor = system.dispatcher)
      Await.ready(activated,5.seconds)

      val msg = new Order("me","Akka in Action",10)
      val xml =
        <order>
          <customerId>{msg.customerId}</customerId>
          <productId>{msg.productId}</productId>
          <number>{msg.number}</number>
        </order>

      val msgFile = new File(dir,"msg1.xml")

      FileUtils.write(msgFile,xml.toString())

      probe.expectMsg(msg)

      system.stop(consumer)

    }

    /*

    "pickup Xml using a TCP connection" in {

      implicit val executionContext = system.dispatcher
      val probe = TestProbe()
      val camelUri =
        "mina:tcp://localhost:8888?textline=true&sync=false"
      val consumer = system.actorOf(
        Props(new OrderConsumerXml(camelUri,probe.ref)))

      val camelExtension = CamelExtension(system)
      val activated = camelExtension
                      .activationFutureFor(consumer)(timeout = 10.seconds,executor = executionContext)
      Await.ready(activated,5.seconds)

      val msg = new Order("me", "Akka in Action", 10)
      val xml = <order>
        <customerId>{ msg.customerId }</customerId>
        <productId>{ msg.productId }</productId>
        <number>{ msg.number }</number>
      </order>

      val xmlStr = xml.toString().replace("\n","")

      val sock = new Socket("localhost",8888)
      val outWriter = new PrintWriter(sock.getOutputStream,true)

      outWriter.println(xmlStr)
      outWriter.flush()

      probe.expectMsg(msg)

      outWriter.close()
      system.stop(consumer)

    }

    */

    "pickup xml ActiveMQ" in {

      val probe = TestProbe()

      val camel = CamelExtension(system)
      val camelContext = camel.context

      val activeMQComponent= ActiveMQComponent
        .activeMQComponent("vm:(broker:(tcp://localhost:8899)?persistent=false)")

      camelContext.addComponent("activemq",activeMQComponent)

      val camelUri = "activemq:queue:xmlTest"
      val consumer = system.
        actorOf(Props(new OrderConsumerXml(camelUri,probe.ref)))

      val activated = camel
                        .activationFutureFor(consumer)(5.seconds,system.dispatcher)
      Await.ready(activated,5.seconds)

      val msg = new Order("me", "Akka in Action", 10)
      val xml = <order>
        <customerId>{ msg.customerId }</customerId>
        <productId>{ msg.productId }</productId>
        <number>{ msg.number }</number>
      </order>


      sendMQMessage(xml.toString())

      probe.expectMsg(msg)

      system.stop(consumer)

    }


    def sendMQMessage(msg: String): Unit = {

      val connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:8899")

      val connection : Connection = connectionFactory.createConnection()
      connection.start()

      val session = connection.createSession(false,Session.AUTO_ACKNOWLEDGE)

      val destination = session.createQueue("xmlTest")

      val producer = session.createProducer(destination)
      producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT)

      val message = session.createTextMessage(msg)

      producer.send(message)

      session.close()
      connection.close()

    }


  }

}
