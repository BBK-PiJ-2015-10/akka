package record.analyzer.state

trait GlobalCommands

// TODO make the records and optional with a default of empty so first caller does not need to pass anything on it

case class ProcessJob(destination: String, sources: Set[String]) extends GlobalCommands
case object FetchRecords extends GlobalCommands
case object FetchRecord extends GlobalCommands

