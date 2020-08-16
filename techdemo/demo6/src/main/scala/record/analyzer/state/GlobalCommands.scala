package record.analyzer.state

trait GlobalCommands

case object FetchRecords extends GlobalCommands
case object FetchRecord extends GlobalCommands
case object DoneWithJob extends GlobalCommands

