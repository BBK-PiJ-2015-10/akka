package record.analyzer.state

import akka.actor.{Actor, ActorRef, DeadLetter, FSM, PoisonPill, Props}
import akka.cluster.routing.{ClusterRouterPool, ClusterRouterPoolSettings}
import akka.routing.{BroadcastPool, RoundRobinPool, SmallestMailboxPool}
import record.analyzer.channel.DoneSourceMessageBus
import record.analyzer.model.{ProcessedRecord, SubmittedRecord}
import record.analyzer.service.RecordProcessor.{DoneProcessingOrphanRecords, ProcessOrphanRecords}
import record.analyzer.service.{RecordProcessor, RecordSink}

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

  context.system.eventStream.subscribe(self,classOf[DeadLetter])

  val sink = context.actorOf(
    ClusterRouterPool(RoundRobinPool(4),ClusterRouterPoolSettings(
      totalInstances = 1000,
      maxInstancesPerNode = 2,
      allowLocalRoutees = false
    )).props(RecordSink.props(destination)))

  //val sink = context.actorOf(SmallestMailboxPool(2).props(RecordSink.props(destination)))
  val processor = context.actorOf(RecordProcessor.props(sources))
  var sourceControllers : Set[String] = initializeControllers(sources)

  private def initializeControllers(sources: Set[String])  = {
    sources.foreach { source =>
      val controller = context.actorOf(SourceController.props(source))
      controller ! FetchRecords
    }
    sources
  }

  startWith(ProcessingRecords,CoordinatorStateData())

  initialize()

  when(ProcessingRecords){
    case Event(DoneSource(source,_), _) => {
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
    case Event(deadLetter: DeadLetter, _) => {
      log.warning(s"ItOps save me please, need to deal with message ${deadLetter.message} sentBy ${deadLetter.sender} sentTo ${deadLetter.recipient}")
      // FORWARD THIS HOT POTATO TO ItOps
      stay()
    }
    case Event(SubmittedRecord,_) => {
      log.info(s"Received a SubmittedRecord from actor ${sender()}")
      stay()
    }
    case Event(message,_) => {
      log.warning(s"Coordinator While on ${stateName} received a weird message ${message} from  ${sender()}")
      stay()
    }
  }

}





