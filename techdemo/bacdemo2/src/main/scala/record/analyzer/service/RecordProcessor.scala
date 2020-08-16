package record.analyzer.service

import akka.actor.{ActorLogging,Props}
import akka.persistence.{PersistentActor, SnapshotOffer}

import record.analyzer.channel.RecordMessageBus
import record.analyzer.model.{NormalRecord, ProcessedRecord}
import record.analyzer.state.{FetchedRecord}


object RecordProcessor {

  def props(sources: Set[String]) : Props = Props(new RecordProcessor(sources))

  case object ProcessOrphanRecords
  case object DoneProcessingOrphanRecords

  sealed trait Event
  case class NormalRecordProcessedEvent(normalRecord: NormalRecord) extends Event
  case object OrphanRecordsProcessedEvent extends Event

  case class Snapshot(records: Set[NormalRecord])

}

class RecordProcessor(sources: Set[String]) extends PersistentActor with ActorLogging{

  import RecordProcessor._

  sources.foreach(source => RecordMessageBus.subscribe(self,source))

  override def persistenceId: String = "recordProcessor"

  var records : Set[NormalRecord] = Set[NormalRecord]()

  override def receiveCommand: Receive = {
    case fetchedRecord : FetchedRecord =>
      persist(NormalRecordProcessedEvent(fetchedRecord.record))(processRecord)
    case ProcessOrphanRecords =>
      persist(OrphanRecordsProcessedEvent) { event =>
        processRecord(event)
        saveSnapshot(RecordProcessor.Snapshot(records))
      }
  }

  override def receiveRecover: Receive = {
    case event: Event => processRecoveredRecord(event)
    case SnapshotOffer(_, snapshot: RecordProcessor.Snapshot) =>
      log.info(s"Recovering NormalRecords from snapshot: $snapshot for $persistenceId")
      records = snapshot.records
  }

  val processRecord : Event => Unit = {
    case NormalRecordProcessedEvent(normalRecord) => {
      if (records.contains(normalRecord)) {
        records -= normalRecord
        context.parent ! ProcessedRecord("joined", normalRecord.id)
      } else {
        records += normalRecord
      }
    }
    case OrphanRecordsProcessedEvent => {
      records.foreach(record => context.parent ! ProcessedRecord("orphaned",record.id))
      records = Set()
      context.parent ! DoneProcessingOrphanRecords
    }

  }

  val processRecoveredRecord : Event => Unit = {
    case NormalRecordProcessedEvent(normalRecord) =>
      if (records.contains(normalRecord)) {
        records -= normalRecord
      } else {
        records += normalRecord
      }
    case OrphanRecordsProcessedEvent =>
      records = Set()
  }

}
