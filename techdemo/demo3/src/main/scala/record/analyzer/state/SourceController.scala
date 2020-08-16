package record.analyzer.state

import akka.actor.{Actor, ActorRef, DeadLetter, FSM, PoisonPill, Props}
import record.analyzer.channel.{DoneSourceMessageBus, RecordMessageBus}
import record.analyzer.model.{DoneRecord, InvalidRecord, NormalRecord}
import record.analyzer.service.{RecordSource}

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


//https://github.com/krasserm/streamz/blob/master/streamz-camel-akka/README.md
//https://doc.akka.io/docs/akka/current/stream/actor-interop.html
//https://doc.akka.io/docs/akka/2.5.4/scala/stream/stream-integrations.html

//https://doc.akka.io/docs/akka/current/distributed-pub-sub.html

class SourceController(source: String) extends Actor with FSM[ControllerState,ControllerStateData]{

  startWith(Idle,ControllerStateData())

  when(Idle){
    case Event(FetchRecords,_) => {
      val fetcher = context.actorOf(RecordSource.props(source))
      //val fetcher = context.actorOf(ActorSource.props(source))
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
      log.warning(s"While on ${stateName} received a weird message ${message}")
      context.system.deadLetters ! DeadLetter(message,sender(),self)
      stay()
    }
  }

}
