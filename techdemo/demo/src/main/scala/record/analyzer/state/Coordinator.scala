package record.analyzer.state

import akka.actor.{Actor, ActorRef, FSM, PoisonPill, Props}
import akka.routing.SmallestMailboxPool
import record.analyzer.channel.DoneSourceMessageBus
import record.analyzer.model.{ProcessedRecord, SubmittedRecord}
import record.analyzer.service.RecordProcessor.{DoneProcessingOrphanRecords, ProcessOrphanRecords}
import record.analyzer.service.{RecordProcessor, RecordSink, RecordSource}

sealed trait CoordinatorState

case object ProcessingRecords extends CoordinatorState
case object ProcessingOrphanRecords extends CoordinatorState
case object Finalizing extends CoordinatorState
case object Done extends CoordinatorState

case class CoordinatorStateData()

case object DoneSources


object Coordinator {

  def props(sources: Set[String], destination: String) = Props(new Coordinator(sources,destination))

}


class Coordinator(sources: Set[String], destination: String) extends Actor with FSM[CoordinatorState,CoordinatorStateData]{

  sources.foreach(source => DoneSourceMessageBus.subscribe(self,source))

  val sink = context.actorOf(SmallestMailboxPool(2).props(RecordSink.props(destination)))
  val processor = context.actorOf(RecordProcessor.props(sources))
  var sourceControllers : Set[String] = initializeControllers(sources)

  private def initializeControllers(sources: Set[String])  = {
    val sourceControllers = sources
    sources.foreach { source =>
      val controller = context.actorOf(SourceController.props(source))
      controller ! FetchRecords
    }
    sourceControllers
  }

  startWith(ProcessingRecords,CoordinatorStateData())

  initialize()

  when(ProcessingRecords){
    case Event(DoneSource(source,actorRef), _) => {
      sourceControllers -= source
      log.info(s"Done received from ${source}, controllers size is ${sourceControllers.size}")
      //actorRef ! PoisonPill
      if (sourceControllers.isEmpty) goto (ProcessingOrphanRecords) else stay()
    }
    case Event(processedRecord: ProcessedRecord, _) => {
      sink ! processedRecord
      stay()
    }
    case Event(SubmittedRecord, _) => {
      stay()
    }
  }

  when(ProcessingOrphanRecords){
    case Event(processedRecord: ProcessedRecord, _) => {
      sink ! processedRecord
      stay()
    }
    case Event(DoneProcessingOrphanRecords, _) => {
      //sender() ! PoisonPill
      goto(Finalizing)
    }
  }

  onTransition {
    case ProcessingRecords -> ProcessingOrphanRecords => {
      processor ! ProcessOrphanRecords
    }
    case ProcessingOrphanRecords -> Finalizing => {
      //sink ! PoisonPill
      //context.parent ! Done
      //self ! PoisonPill
    }
  }

  whenUnhandled {
    case Event(message,_) => {
      log.warning(s"While on ${stateName} received a weird message ${message}")
      stay()
    }
  }

}





