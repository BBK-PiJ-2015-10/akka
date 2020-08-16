package aia.channels


import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.actor.{ActorSystem, DeadLetter, PoisonPill, Props}
import org.scalatest.BeforeAndAfterAll
import java.util.Date

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

//
https://doc.akka.io/docs/akka/current/multi-jvm-testing.html

class ProxyMultiJvm extends TestKit(ActorSystem("DeadLetterTest"))
with AnyWordSpecLike with BeforeAndAfterAll with ImplicitSender
with Matchers{


  override protected def afterAll(): Unit = {
    system.terminate()
  }

  "DeadLetter" must {

    "catch messages send to deadLetters" in {


      val deadLetterMonitor = TestProbe()

      system.eventStream.subscribe(deadLetterMonitor.ref, classOf[DeadLetter])


      val msg = new StateEvent(new Date(), "Connected")
      system.deadLetters ! msg

      val dead = deadLetterMonitor.expectMsgType[DeadLetter]
      dead.message.mustBe(msg)
      dead.sender.mustBe(testActor)
      dead.recipient must be(system.deadLetters)

    }

  }

}
