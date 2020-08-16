package aia.channels

import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.actor.{ActorSystem, DeadLetter, PoisonPill, Props}
import org.scalatest.BeforeAndAfterAll
import java.util.Date

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike



class DeadLetterTest extends TestKit(ActorSystem("DeadLetterTest"))
    with AnyWordSpecLike with BeforeAndAfterAll with ImplicitSender
    with Matchers  {


  override protected def afterAll(): Unit = {
    system.terminate()
  }

  "DeadLetter" must {

    "catch messages send to deadLetters" in {


      val deadLetterMonitor = TestProbe()

      system.eventStream.subscribe(deadLetterMonitor.ref,classOf[DeadLetter])


      val msg = new StateEvent(new Date(),"Connected")
      system.deadLetters ! msg

      val dead = deadLetterMonitor.expectMsgType[DeadLetter]
      dead.message.mustBe(msg)
      dead.sender.mustBe(testActor)
      dead.recipient  must be (system.deadLetters)

    }

    "catch deadLetter messages send to deadLetters" in {

      val deadLetterMonitor = TestProbe()
      val actor = system.actorOf(Props[EchoActor],"echo")

      system.eventStream
        .subscribe(deadLetterMonitor.ref,classOf[DeadLetter])

      val msg = new Order("me","Akka in Action",1)
      val dead = DeadLetter(msg,testActor,actor)
      system.deadLetters ! dead

      deadLetterMonitor.expectMsg(dead)

      system.stop(actor)

    }


    "catch messages send to terminated Actor" in {

      val deadLetterMonitor = TestProbe()

      system.eventStream
        .subscribe(deadLetterMonitor.ref,classOf[DeadLetter])

      val actor = system.actorOf(Props[EchoActor],"echo")
      actor ! PoisonPill

      val msg = new Order("me","Akka in Action",1)
      actor ! msg

      val dead = deadLetterMonitor.expectMsgType[DeadLetter]
      dead.message must be (msg)
      dead.sender must be (testActor)
      dead.recipient must be (actor)

    }



  }


}
