package akka.pattern.throttle


import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern.throttle.Throttler.Rate
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import akka.pattern.throttle.Throttler._

import scala.concurrent.duration.FiniteDuration

object TimerBasedThrottlerSpec {

  class EchoActor extends Actor {

    def receive : Receive = {
      case x => sender ! x
    }
  }

}

class TimerBasedThrottlerSpec extends TestKit(ActorSystem("TimerBasedThrottlerTest"))
  with AnyWordSpecLike with BeforeAndAfterAll with Matchers
  with ImplicitSender
{
  override protected def afterAll(): Unit = system.terminate()

  "A throttler" must {

    "must pass the ScalaDoc class documentation example program" in {

      val printer = system.actorOf(Props(new Actor {
        override def receive: Receive = {
          case x => println(x)
        }
      }))

      val rate = Rate(3,FiniteDuration(1,TimeUnit.SECONDS))

      val throttler = system.actorOf(Props(new TimerBasedThrottler(rate)))

      throttler ! SetTarget(Some(printer))

      throttler ! Queue("1")
      throttler ! Queue("2")
      throttler ! Queue("3")
      throttler ! Queue("4")
      throttler ! Queue("5")

    }

    "keep messages until a target is set" in {
      val probeActor = TestProbe()
      val rate = Rate(3,FiniteDuration(1,TimeUnit.SECONDS))
      val throttler = system.actorOf(Props(new TimerBasedThrottler(rate)))
      throttler ! Queue("1")
      throttler ! Queue("2")
      throttler ! Queue("3")
      throttler ! Queue("4")
      throttler ! Queue("5")
      throttler ! Queue("6")
      expectNoMsg(FiniteDuration(1,TimeUnit.SECONDS))
      throttler ! SetTarget(Some(probeActor.ref))
      within(FiniteDuration(2,TimeUnit.SECONDS)) {
        probeActor.expectMsg("1")
        probeActor.expectMsg("2")
        probeActor.expectMsg("3")
        probeActor.expectMsg("4")
        probeActor.expectMsg("5")
        probeActor.expectMsg("6")
      }
    }


  }

  "respect the rate (3 msg/s)" in {
    val probeActor = TestProbe()
    val rate = Rate(3,FiniteDuration(1,TimeUnit.SECONDS))
    val throttler = system.actorOf(Props(new TimerBasedThrottler(rate)))
    throttler ! SetTarget(Some(probeActor.ref))
    throttler ! Queue("a")
    throttler ! Queue("b")
    throttler ! Queue("c")
    throttler ! Queue("d")
    throttler ! Queue("e")
    throttler ! Queue("f")
    throttler ! Queue("g")
    within(FiniteDuration(3,TimeUnit.SECONDS)) {
      probeActor.expectMsg("a")
      probeActor.expectMsg("b")
      probeActor.expectMsg("c")
    }
    within(FiniteDuration(3,TimeUnit.SECONDS)) {
      probeActor.expectMsg("d")
      probeActor.expectMsg("e")
      probeActor.expectMsg("f")
    }
    within(FiniteDuration(3,TimeUnit.SECONDS)) {
      probeActor.expectMsg("g")
    }
  }

}

