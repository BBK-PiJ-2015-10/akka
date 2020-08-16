package aia.faulttolerance

import aia.faulttolerance.LifeCycleHooks.{ForceRestart,SampleMessage}

import akka.actor.{ActorSystem,Props,ActorRef}

import akka.testkit.TestKit

import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class LifeCycleHookTest
  extends TestKit(ActorSystem("LifeCyleTest"))
  with AnyWordSpecLike with BeforeAndAfterAll {

  override protected def afterAll(): Unit = system.terminate()

  "The child" must {

    "log cycle hooks" in {

      val testActorRef : ActorRef = system.actorOf(Props[LifeCycleHooks],"LifeCycleHooks")
      watch(testActorRef)

      testActorRef ! ForceRestart
      testActorRef tell(SampleMessage,testActor)
      expectMsg(SampleMessage)
      system.stop(testActorRef)
      expectTerminated(testActorRef)

    }

  }


}
