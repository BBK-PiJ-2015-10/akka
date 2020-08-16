package aia.state

import akka.testkit.{TestProbe,ImplicitSender,TestKit}

import akka.actor.{ActorSystem,Props,ActorRef}
import akka.actor.FSM.{Transition,CurrentState,SubscribeTransitionCallBack}

import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll}
import org.scalatest.matchers.must.Matchers

import concurrent.duration._

import java.util.concurrent.TimeUnit
//import scala.language.postfixOps


class InventoryTest extends TestKit(ActorSystem("InventoryTest"))
  with AnyWordSpecLike with BeforeAndAfterAll with Matchers
  with ImplicitSender {

  override protected def afterAll(): Unit = system.terminate()

  "Inventory" must {
    "follow the flow" in {
      val publisher : ActorRef = system.actorOf(Props(new Publisher(2,2)))
      val inventory : ActorRef = system.actorOf(Props(new Inventory(publisher)))
      val stateProbe : TestProbe = TestProbe()
      val replyProbe : TestProbe = TestProbe()

      inventory ! new SubscribeTransitionCallBack(stateProbe.ref)
      stateProbe.expectMsg(
        new CurrentState(inventory,WaitForRequests)
      )

      //start test
      inventory ! new BookRequest("context1",replyProbe.ref)
      stateProbe.expectMsg(
        new Transition(inventory,WaitForRequests,WaitForPublisher)
      )
      stateProbe.expectMsg(
        new Transition(inventory,WaitForPublisher,ProcessRequest)
      )
      stateProbe.expectMsg(
        new Transition(inventory,ProcessRequest,WaitForRequests)
      )
      replyProbe.expectMsg(
        new BookReply("context1",Right(1))
      )

      inventory ! new BookRequest("context2",replyProbe.ref)
      stateProbe.expectMsg(
        new Transition(inventory,WaitForRequests,ProcessRequest)
      )
      stateProbe.expectMsg(
        new Transition(inventory,ProcessRequest,WaitForRequests)
      )
      replyProbe.expectMsg(
        new BookReply("context2",Right(2))
      )

      inventory ! new BookRequest("context3",replyProbe.ref)
      stateProbe.expectMsg(
        new Transition(inventory, WaitForRequests,WaitForPublisher)
      )
      stateProbe.expectMsg(
        new Transition(inventory,WaitForPublisher,ProcessSoldOut)
      )
      replyProbe.expectMsg(
        new BookReply("context3",Left("SoldOut"))
      )
      stateProbe.expectMsg(
        new Transition(inventory,ProcessSoldOut,SoldOut)
      )

      inventory ! new BookRequest("context4",replyProbe.ref)
      stateProbe.expectMsg(
        new Transition(inventory,SoldOut,ProcessSoldOut)
      )
      replyProbe.expectMsg(
        new BookReply("context4",Left("SoldOut"))
      )
      stateProbe.expectMsg(
        new Transition(inventory,ProcessSoldOut,SoldOut)
      )
      system.stop(inventory)
      system.stop(publisher)

    }


    "process multiple request" in {

      val publisher : ActorRef = system.actorOf(Props(new Publisher(2,2)))
      val inventory : ActorRef = system.actorOf(Props(new Inventory(publisher)))
      val stateProbe : TestProbe = TestProbe()
      val replyProbe : TestProbe = TestProbe()

      inventory ! new SubscribeTransitionCallBack(stateProbe.ref)

      stateProbe.expectMsg(
        new CurrentState(inventory,WaitForRequests)
      )

      inventory ! new BookRequest("context1",replyProbe.ref)
      inventory ! new BookRequest("context2",replyProbe.ref)

      stateProbe.expectMsg(
        new Transition(inventory,WaitForRequests,WaitForPublisher)
      )
      stateProbe.expectMsg(
        new Transition(inventory,WaitForPublisher,ProcessRequest)
      )
      stateProbe.expectMsg(
        new Transition(inventory,ProcessRequest,WaitForRequests)
      )
      replyProbe.expectMsg(
        new BookReply("context1",Right(1))
      )
      stateProbe.expectMsg(
        new Transition(inventory,WaitForRequests,ProcessRequest)
      )
      stateProbe.expectMsg(
        new Transition(inventory,ProcessRequest,WaitForRequests)
      )
      replyProbe.expectMsg(
        new BookReply("context2",Right(2))
      )

      system.stop(inventory)
      system.stop(publisher)


    }

  }

  "support multiple supplies" in {

    val publisher : ActorRef = system.actorOf(Props(new Publisher(4,2)))
    val inventory : ActorRef = system.actorOf(Props(new Inventory(publisher)))
    val stateProbe : TestProbe = TestProbe()
    val replyProbe : TestProbe = TestProbe()

    inventory ! new SubscribeTransitionCallBack(stateProbe.ref)
    stateProbe.expectMsg(
      new CurrentState(inventory,WaitForRequests)
    )

    inventory ! new BookRequest("context1",replyProbe.ref)
    stateProbe.expectMsg(
      new Transition(inventory,WaitForRequests,WaitForPublisher)
    )
    stateProbe.expectMsg(
      new Transition(inventory,WaitForPublisher,ProcessRequest)
    )
    stateProbe.expectMsg(
      new Transition(inventory,ProcessRequest,WaitForRequests)
    )
    replyProbe.expectMsg(
      new BookReply("context1",Right(1))
    )

    inventory ! new BookRequest("context2",replyProbe.ref)
    stateProbe.expectMsg(
      new Transition(inventory,WaitForRequests,ProcessRequest)
    )
    stateProbe.expectMsg(
      new Transition(inventory,ProcessRequest,WaitForRequests)
    )
    replyProbe.expectMsg(
      new BookReply("context2",Right(2))
    )

    inventory ! new BookRequest("context3",replyProbe.ref)
    stateProbe.expectMsg(
      new Transition(inventory,WaitForRequests,WaitForPublisher)
    )
    stateProbe.expectMsg(
      new Transition(inventory,WaitForPublisher,ProcessRequest)
    )
    stateProbe.expectMsg(
      new Transition(inventory,ProcessRequest,WaitForRequests)
    )
    replyProbe.expectMsg(
      new BookReply("context3",Right(3))
    )

    inventory ! new BookRequest("context4",replyProbe.ref)
    stateProbe.expectMsg(
      new Transition(inventory,WaitForRequests,ProcessRequest)
    )
    stateProbe.expectMsg(
      new Transition(inventory,ProcessRequest,WaitForRequests)
    )
    replyProbe.expectMsg(
      new BookReply("context4",Right(4))
    )

    system.stop(publisher)
    system.stop(inventory)


  }

  "InventoryTimer" must {

    "follow the flow" in {

      val publisher = TestProbe()
      val inventory = system.actorOf(Props(new InventoryWithTimer(publisher.ref)))
      val stateProbe = TestProbe()
      val replyProbe = TestProbe()

      inventory ! new SubscribeTransitionCallBack(stateProbe.ref)
      stateProbe.expectMsg(
        new CurrentState(inventory,WaitForRequests)
      )

      //start test
      inventory ! new BookRequest("context1",replyProbe.ref)
      stateProbe.expectMsg(
        new Transition(inventory,WaitForRequests,WaitForPublisher)
      )
      publisher.expectMsg(PublisherRequest)
      stateProbe.expectMsg(
        FiniteDuration(6,TimeUnit.SECONDS),new Transition(inventory,WaitForPublisher,WaitForRequests)
      )
      stateProbe.expectMsg(
        new Transition(inventory,WaitForRequests,WaitForPublisher)
      )

      system.stop(inventory)

    }

  }


}
