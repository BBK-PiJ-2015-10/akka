package record.analyzer.state

import akka.actor.{Actor, ActorRef, FSM, PoisonPill, Props}
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


class SourceController(source: String) extends Actor with FSM[ControllerState,ControllerStateData]{

  startWith(Idle,ControllerStateData())

  when(Idle){
    case Event(FetchRecords,_) => {
      val fetcher = context.actorOf(RecordSource.props(source))
      fetcher ! FetchRecord
      goto(WaitingForFetcher)
    }
  }

  when(WaitingForFetcher) {
    case Event(record: NormalRecord, _) => {
      val fetchedRecord = FetchedRecord(record,source)
      RecordMessageBus.publish(fetchedRecord)
      log.info(s"Published ${fetchedRecord}")
      sender ! FetchRecord
      stay()
    }
    case Event(DoneRecord, _)   => {
      val doneSource = DoneSource(source,self)
      DoneSourceMessageBus.publish(doneSource)
      log.info(s"Published ${doneSource}")
      goto(Idle)
    }
    case Event(invalidRecord: InvalidRecord, _) => {
      log.info(s"Received an invalid record ${invalidRecord}")
      sender() ! FetchRecord
      stay()
    }

  }

  initialize()

  onTransition {
    case WaitingForFetcher -> Idle => {
      context.children.foreach(fetcher => fetcher ! PoisonPill)
    }
  }

  whenUnhandled {
    case Event(message,_) => {
      log.warning(s"While on ${stateName} received a weird message ${message}")
      // publish to a DLQ event stream
      stay()
    }
  }

}
