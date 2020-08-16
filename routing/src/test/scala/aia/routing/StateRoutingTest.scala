package aia.routing

import java.util.concurrent.TimeUnit

import scala.concurrent.duration._
import akka.actor.{ActorSystem, Props}
import akka.remote.WireFormats
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike


class StateRoutingTest
  extends TestKit(ActorSystem("StateRoutingTest"))
  with AnyWordSpecLike with BeforeAndAfterAll {

  override protected def afterAll(): Unit = {
    system.terminate()
  }

  "The router" must {
    "routes depending on state" in {

      val normalFlowProbe = TestProbe()
      val cleanUpProbe = TestProbe()
      var router =
        system.actorOf(Props(new SwitchRouter(normalFlow = normalFlowProbe.ref, cleanUp = cleanUpProbe.ref)))

      val timeLimit = new FiniteDuration(1,TimeUnit.SECONDS)

      val msg = "message"
      router ! msg

      cleanUpProbe.expectMsg(msg)
      normalFlowProbe.expectNoMessage(timeLimit)

      router ! RouteStateOn

      router ! msg
      normalFlowProbe.expectMsg(msg)
      cleanUpProbe.expectNoMessage(timeLimit)

      router ! RouteStateOff
      router ! msg

      cleanUpProbe.expectMsg(msg)
      normalFlowProbe.expectNoMessage(timeLimit)

    }

    "routes2 pending on state" in {

      val normalFlowProbe = TestProbe()
      val cleanUpProbe = TestProbe()
      val router = system.actorOf(Props(new SwitchRouter2(normalFlow = normalFlowProbe.ref, cleanUp = cleanUpProbe.ref)))

      val msg = "message"
      router ! msg

      val timeLimit = new FiniteDuration(1,TimeUnit.SECONDS)

      cleanUpProbe.expectMsg(msg)
      normalFlowProbe.expectNoMessage(timeLimit)


      router ! RouteStateOn
      router ! msg

      cleanUpProbe.expectNoMessage(timeLimit)
      normalFlowProbe.expectMsg(msg)

      router ! RouteStateOff
      router ! msg

      cleanUpProbe.expectMsg(msg)
      normalFlowProbe.expectNoMessage(timeLimit)


    }

    "log wrong stateChange request" in {

      val normalFlowProbe = TestProbe()
      val cleanUpProbe = TestProbe()
      val router = system.actorOf(Props(new SwitchRouter(normalFlow = normalFlowProbe.ref,cleanUp = cleanUpProbe.ref)))

      router ! RouteStateOff

      router ! RouteStateOn

      router ! RouteStateOn



    }

  }

}
