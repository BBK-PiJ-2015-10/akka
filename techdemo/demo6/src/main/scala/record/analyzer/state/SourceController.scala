package record.analyzer.state

import akka.actor.{Actor, ActorRef, DeadLetter, FSM, PoisonPill, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, SubscribeAck}
import record.analyzer.channel.{DoneSourceMessageBus, RecordMessageBus}
import record.analyzer.model.{DoneRecord, InvalidRecord, NormalRecord}
import record.analyzer.service.RecordSource

sealed trait ControllerState

case object Idle extends ControllerState
case object WaitingForFetcher extends ControllerState

case class ControllerStateData()

sealed trait ControllerStateCommand

case class FetchedRecord(record: NormalRecord, source: String)
case class DoneSource(source: String, actor: ActorRef)

object SourceController {
  def props(source: String) = Props(new SourceController(source))
}


class SourceController(source: String) extends FSM[ControllerState,ControllerStateData]{

  val mediator : ActorRef = DistributedPubSub(context.system).mediator

  startWith(Idle,ControllerStateData())

  when(Idle){
    case Event(FetchRecords,_) => {
      val fetcher = context.actorOf(RecordSource.props(source),"fetcher-"+source.substring(22))
      //val fetcher = context.actorOf(ActorSource.props(source))
      fetcher ! FetchRecord
      goto(WaitingForFetcher)
    }
  }

  when(WaitingForFetcher) {
    case Event(record: NormalRecord, _) => {
      val fetchedRecord = FetchedRecord(record,source)
      //context.system.eventStream.publish(fetchedRecord)
      //RecordMessageBus.publish(fetchedRecord)
      mediator ! Publish("records",fetchedRecord)
      log.info(s"Actor with name ${self.path.name} published ${fetchedRecord}")
      sender ! FetchRecord
      stay()
    }
    case Event(DoneRecord, _)   => {
      val doneSource = DoneSource(source,self)
      //context.system.eventStream.publish(doneSource)
      //DoneSourceMessageBus.publish(doneSource)
      mediator ! Publish("donesource",doneSource)
      log.info(s"Actor with name ${self.path.name} published ${doneSource}")
      goto(Idle)
    }
    case Event(invalidRecord: InvalidRecord, _) => {
      log.info(s"Actor with name ${self.path.name} received an invalid record ${invalidRecord}")
      context.system.deadLetters ! DeadLetter(invalidRecord,sender(),self)
      sender() ! FetchRecord
      stay()
    }

  }

  initialize()

  onTransition {
    case WaitingForFetcher -> Idle => {
      context.children.foreach(fetcher => fetcher ! PoisonPill)
      // self ! PoisonPill
    }
  }

  whenUnhandled {
    case Event(message,_) => {
      log.warning(s"While on ${stateName} actor with name ${self.path.name} received a weird message ${message}")
      context.system.deadLetters ! DeadLetter(message,sender(),self)
      stay()
    }
  }

}
