package record.analyzer.model

sealed trait Record

case class NormalRecord(status: String, id: String) extends Record
case object DoneRecord extends Record
case class InvalidRecord(record: String) extends Record
case class ProcessedRecord(kind: String, id: String) extends Record
case object SubmittedRecord extends Record
