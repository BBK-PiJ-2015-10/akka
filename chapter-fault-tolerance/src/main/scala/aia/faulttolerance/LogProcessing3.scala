package aia.faulttolerance

import java.io.File
import java.util.UUID


import language.postfixOps
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props, SupervisorStrategy, Terminated}
import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.OneForOneStrategy

import scala.concurrent.duration._

package dbstrategy3 {

  import akka.actor.AllForOneStrategy


  object LogProcessingApp3 extends App {


    val sources = Vector("file:///source1/","file:///source2/")
    val system = ActorSystem("logProcessing2")

    val databaseUrl = "http://mydatabase"

    val writerProps : Props = Props(new DbWriter(databaseUrl))
    val dbSupervisorProps : Props = Props(new DbSupervisor(writerProps))
    val logProcSuperProps : Props = Props(new LogProcessorSupervisor(dbSupervisorProps))
    val topLevelProps : Props = Props(new FileWatcherSupervisor(sources,logProcSuperProps))
    system.actorOf(topLevelProps)

  }



  class FileWatcherSupervisor(sources: Vector[String], logProcSuperProps: Props) extends Actor {

    var fileWatchers : Vector[ActorRef] = sources.map { source =>
      val logProcessorSupervisor = context.actorOf(logProcSuperProps)
      val fileWatcher = context.actorOf(Props(new FileWatcher(source,logProcessorSupervisor)))
      context.watch(fileWatcher)
      fileWatcher
    }

    override def supervisorStrategy: SupervisorStrategy = AllForOneStrategy() {
      case _: DiskError => Stop
    }

    override def receive: Receive = {
      case Terminated(fileWatcher) =>
        fileWatchers = fileWatchers.filterNot(_ == fileWatcher)
        if (fileWatchers.isEmpty) self ! PoisonPill
    }

  }

  class FileWatcher(sourceUri: String, logProcessingSupervisor: ActorRef)
    extends Actor with FileWatchingCapabilities {

    register(sourceUri)

    import FileWatcherProtocol.{NewFile,SourceAbandoned}
    import LogProcessingProtocol.LogFile

    override def receive: Receive = {
      case NewFile(file,_) => logProcessingSupervisor ! LogFile(file)
      case SourceAbandoned(uri) if (uri == sourceUri) => self ! PoisonPill
    }
  }

  class LogProcessorSupervisor(dbSupervisorProps: Props) extends Actor {

    val dbSupervisor : ActorRef = context.actorOf(dbSupervisorProps)
    val logProcProps : Props = Props(new LogProcessor(dbSupervisor))
    val logProcessor : ActorRef = context.actorOf(logProcProps)

    override def receive: Receive = {
      case m => logProcessor forward m
    }

  }

  class LogProcessor(dbSupervisor: ActorRef)
    extends Actor with LogParsing {

    import LogProcessingProtocol.{LogFile,Line}

    override def receive: Receive = {
      case LogFile(file) =>
        val lines : Vector[Line] = parse(file)
        lines.foreach(dbSupervisor ! _)
    }

  }

  class DbImpatientSupervisor(writerProps: Props) extends Actor {

    override def supervisorStrategy: SupervisorStrategy =
      OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 60 seconds) {
      case _:DbBrokenConnectionException => Restart
    }

    val writer = context.actorOf(writerProps)

    override def receive: Receive = {
      case m => writer forward m
    }

  }

  class DbSupervisor(writerProps: Props) extends Actor {

    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
      case _: DbBrokenConnectionException => Restart
    }

    val writer = context.actorOf(writerProps)

    override def receive: Receive = {
      case m => writer forward m
    }
  }

  class DbWriter(databaseUrl: String) extends Actor {

    import LogProcessingProtocol.Line

    val connection = new DbCon(databaseUrl)

    override def receive: Receive = {

      case Line(time,message,messageType) =>
        connection.write(Map('time -> time, 'message -> message, 'messageType -> messageType))
    }

    override def postStop(): Unit = connection.close()

  }

  class DbCon(url: String) {

    /**
      * Writes a map to a database.
      * @param map the map to write to the database.
      * @throws DbBrokenConnectionException when the connection is broken. It might be back later
      * @throws DbNodeDownException when the database Node has been removed from the database cluster. It will never work again.
      */
    def write(map: Map[Symbol,Any]): Unit = {}

    def close() : Unit = {}

  }

  @SerialVersionUID(1L)
  class DiskError(msg: String) extends Error(msg) with Serializable

  @SerialVersionUID(1L)
  class CorruptedFileException(msg: String, val file: File) extends Exception(msg) with Serializable

  @SerialVersionUID(1L)
  class DbBrokenConnectionException(msg: String) extends Exception(msg) with Serializable

  @SerialVersionUID(1L)
  class DbNodeDownException(msg: String) extends Exception(msg) with Serializable

  trait LogParsing {
    import LogProcessingProtocol.Line

    // Parses log files. creates line objects from the lines in the log file.
    // If the file is corrupt a CorruptedFileException is thrown
    // implement parser here, now just return dummy value
    def parse(file: File): Vector[Line] = { Vector.empty[Line]}

  }

  object FileWatcherProtocol {

    case class NewFile(file: File, timeAdded: Long)
    case class SourceAbandoned(uri: String)

  }

  trait FileWatchingCapabilities {

    def register(uri: String): Unit = {}

  }

  object LogProcessingProtocol {

    //represents a new log file
    case class LogFile(file: File)

    // A line in the log file parsed by the LogProcessor Actor
    case class Line(time: Long, message: String, messageType: String)

  }

}
