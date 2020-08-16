package aia.routing

import java.util.concurrent.TimeUnit

import scala.concurrent.duration._
import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class MsgRoutingTest
  extends TestKit(ActorSystem("MsgRoutingTest"))
  with AnyWordSpecLike with BeforeAndAfterAll {

  override protected def afterAll(): Unit = {
    system.terminate()
  }

  "The Router must" in {

    val normalFlowProbe = TestProbe()
    val cleanUpProbe = TestProbe()
    val router =
      system.actorOf(Props.empty.withRouter
                        (new SpeedRouterPool(
                          50,
                          Props(new RedirectActor(normalFlowProbe.ref)),
                          Props(new RedirectActor(cleanUpProbe.ref)))
                        ))

    val msg = new Photo(license = "123xyz", speed =60)
    router ! msg

    val timeLimit = new FiniteDuration(1,TimeUnit.SECONDS)

    cleanUpProbe.expectNoMessage(timeLimit)
    normalFlowProbe.expectMsg(msg)

    val msg2 = new Photo(license = "123xyz", speed = 45)
    router ! msg2

    cleanUpProbe.expectMsg(msg2)
    normalFlowProbe.expectNoMessage(timeLimit)





  }


}
