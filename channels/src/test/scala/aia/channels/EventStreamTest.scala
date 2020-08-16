package aia.channels

import scala.concurrent.duration._

import akka.testkit.{TestProbe,TestKit}
import akka.actor.ActorSystem

import java.util.Date

import org.scalatest.{BeforeAndAfterAll}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike


class EventStreamTest
  extends TestKit(ActorSystem("EventStreamTest"))
    with AnyWordSpecLike with BeforeAndAfterAll with Matchers {

  override protected def afterAll() = {
    system.terminate()
  }

  "EventStream" must {

    "distribute messages" in {

      val deliveryOrder = TestProbe()
      val giftModule = TestProbe()

      system.eventStream
        .subscribe(deliveryOrder.ref,classOf[Order])

      system.eventStream
        .subscribe(giftModule.ref,classOf[Order])

      val msg = new Order("me","Akka in Action",2)
      system.eventStream.publish(msg)

      giftModule.expectMsg(msg)
      deliveryOrder.expectMsg(msg)

    }

  "monitor hierarchy" in {

    val giftModule = TestProbe()

    system.eventStream
      .subscribe(giftModule.ref,classOf[Order])

    val msg = new Order("me","Akka in Action",3)
    system.eventStream.publish(msg)

    giftModule.expectMsg(msg)

    val msg2 = new CancelOrder(new Date(),"me","Akka in Action",2)
    system.eventStream.publish(msg2)

    giftModule.expectMsg(msg2)


  }

  "unsubscribe messages" in {

    val deliveryOrder = TestProbe()
    val giftModule = TestProbe()

    system.
      eventStream.
        subscribe(deliveryOrder.ref,classOf[Order])


    system.
      eventStream.
        subscribe(giftModule.ref,classOf[Order])

    val msg = new Order("me","Akka in action",3)
    system.
      eventStream.
        publish(msg)

    deliveryOrder.expectMsg(msg)
    giftModule.expectMsg(msg)

    system
      .eventStream
        .unsubscribe(giftModule.ref)

    system
      .eventStream
        .publish(msg)

    deliveryOrder.expectMsg(msg)
    giftModule.expectNoMessage(3000.millis)

  }

  }

  "myEventBus" must {

    "deliver all messages" in {

      val bus = new MyEventBus
      val systemLog = TestProbe()

      bus.subscribe(systemLog.ref)

      val msg = new Order("me","Akka in Action",3)
      bus.publish(msg)
      systemLog.expectMsg(msg)

      bus.publish("mike")
      systemLog.expectMsg("mike")

    }

  }

  "OrderMessageBus" must {

    "deliver Order messages" in {

      val bus = new OrderMessageBus

      val singleBooks = TestProbe()
      bus.subscribe(singleBooks.ref,false)

      val multiBooks = TestProbe()
      bus.subscribe(multiBooks.ref,true)

      val msg = new Order("me","Akka in Action",1)
      bus.publish(msg)

      singleBooks.expectMsg(msg)
      multiBooks.expectNoMessage(3.seconds)

      val msg2 = new Order("me","Akka in Action",3)
      bus.publish(msg2)

      multiBooks.expectMsg(msg2)
      singleBooks.expectNoMessage(3.seconds)

    }

    "deliver order messages when multiple subscriber" in {

      val bus = new OrderMessageBus

      val listener = TestProbe()

      bus.subscribe(listener.ref,true)
      bus.subscribe(listener.ref,false)

      val msg = new Order("me","Akka in action",1)
      bus.publish(msg)

      listener.expectMsg(msg)

      val msg2 = new Order("me","Akka in action",3)
      bus.publish(msg2)

      listener.expectMsg(msg2)


    }

  }

}


