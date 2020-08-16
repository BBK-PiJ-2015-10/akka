package aia.deploy

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestActors, TestKit}

import org.scalatest.matchers.must.Matchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike


class HelloWorldTest extends TestKit(ActorSystem("HelloWordTest"))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll
{

  override protected def afterAll(): Unit = {
    system.terminate()
  }

  val actor = TestActorRef[HelloWorld]

  "Hello World" must {

    "reply when sending a string" in {

      actor ! "everybody"
      expectMsg("Hello everybody")

    }

  }

}
