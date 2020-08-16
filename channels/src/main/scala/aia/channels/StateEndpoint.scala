package aia.channels

import akka.actor.{Actor, ActorRef}
import java.util.Date

case class StateEvent(time: Date, state: String)
case class Connection(time: Date, connected: Boolean)

class StateEndpoint extends Actor {

  override def receive: Receive = {

    case Connection(time, true) => {
      context.system.eventStream.publish(new StateEvent(time,"Connected"))
    }
    case Connection(time, false) => {
      context.system.eventStream.publish(new StateEvent(time,"Disconnected"))
    }

  }

}


class SystemLog extends Actor{

  override def receive: Receive = {
    case event : StateEvent => {}
  }

}

class SystemMonitor extends  Actor {

  override def receive: Receive = {
    case event: StateEvent => {}
  }

}

import akka.event.ActorEventBus
import akka.event.{LookupClassification,EventBus}

class OrderMessageBus extends EventBus
  with LookupClassification
  with ActorEventBus {

  override type Event = Order

  override type Classifier = Boolean

  override protected def mapSize(): Int = 2

  override protected def classify(event: OrderMessageBus#Event)= {
    event.number > 1
  }

  override protected def publish(event: OrderMessageBus#Event, subscriber: OrderMessageBus#Subscriber) = {
    subscriber ! event
  }


}

class MyEventBus extends EventBus
   with LookupClassification
with ActorEventBus {

  override type Event = AnyRef

  override type Classifier = String

  override protected def mapSize(): Int = 2

  override protected def classify(event: MyEventBus#Event) = {
    "TestBus"
  }

  override protected def publish(event: MyEventBus#Event, subscriber: MyEventBus#Subscriber) = {
    subscriber ! event
  }

  def subscribe(subscriber : Subscriber) : Boolean =
    subscribers.put("TestBus",subscriber)

}



