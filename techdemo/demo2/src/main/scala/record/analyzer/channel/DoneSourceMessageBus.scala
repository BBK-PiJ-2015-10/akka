package record.analyzer.channel

import akka.actor.ActorRef
import akka.event.{ActorEventBus, EventBus, LookupClassification}
import record.analyzer.state.DoneSource

object DoneSourceMessageBus extends EventBus
  with LookupClassification
  with ActorEventBus {

  override type Event = DoneSource

  override type Classifier = String

  override protected def mapSize(): Int = 200

  override protected def classify(event: DoneSource): String = event.source

  override protected def publish(event: DoneSource, subscriber: ActorRef) = subscriber ! event

}
