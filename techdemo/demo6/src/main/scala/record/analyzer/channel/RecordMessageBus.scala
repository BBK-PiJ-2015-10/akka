package record.analyzer.channel

import akka.actor.ActorRef
import akka.event.{ActorEventBus, EventBus, LookupClassification}
import record.analyzer.state.FetchedRecord

object RecordMessageBus extends EventBus
  with LookupClassification
  with ActorEventBus{

  override type Event = FetchedRecord
  override type Classifier = String
  override protected def mapSize = 100

  override protected def classify(event: FetchedRecord): String = event.source

  override protected def publish(event: FetchedRecord, subscriber: ActorRef): Unit = subscriber ! event

}
