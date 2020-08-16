package aia.faulttolerance

import java.io.File;
import java.util.UUID

import language.postfixOps

import akka.actor.{ActorRef,Actor,Props,ActorLogging,ActorSystem,Terminated,PoisonPill,SupervisorStrategy}
import akka.actor.SupervisorStrategy.{Stop,Resume,Restart,Escalate}
import akka.actor.OneForOneStrategy

import scala.concurrent.duration._


package dbstrategy1 {

  object LogProcessingApp extends App {

    //val sources = Vector("file:///source1/", "file:///source2/")
    val sources = Vector("file:///source1/","file:///source2/")
    val system = ActorSystem("logProcessing")

    val databaseUrl = "http://mydatabase1"

    system.actorOf(LogProcessingSupervisor.props(sources,databaseUrl),LogProcessingSupervisor.name)

  }



  object LogProcessingSupervisor {

    def props(sources: Vector[String], databaseUrl: String) : Props = {
      Props(new LogProcessingSupervisor(sources,databaseUrl))
    }

    def name : String = "file-watcher-supervisor"

  }

  class LogProcessingSupervisor(sources: Vector[String], databaseUrl: String)
    extends Actor with ActorLogging {

    var fileWatchers : Vector[ActorRef] = sources.map { source =>
      val dbWriter = context.actorOf(DbWriter.props(databaseUrl),DbWriter.name(databaseUrl))
      val logProcessor = context.actorOf(LogProcessor.props(dbWriter),LogProcessor.name)
      val fileWatcher = context.actorOf(FileWatcher.props(source,logProcessor),FileWatcher.name)
      context.watch(fileWatcher)
      fileWatcher
    }

    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
      case _: CorruptedFileException => Resume
      case _: DbBrokenConnectionException => Restart
      case _: DiskError => Stop
    }

    override def receive: Receive = {
      case Terminated(actorRef: ActorRef) =>
        fileWatchers = fileWatchers.filterNot(_ == actorRef)
        if (fileWatchers.isEmpty){
          log.info("Shutting down, all file watchers have failed.")
          context.system.terminate()
        }
    }

  }


  object FileWatcher {

    def props(source: String, logProcessor: ActorRef): Props = Props(new FileWatcher(source,logProcessor))

    def name: String = s"file-watcher-${UUID.randomUUID.toString}"

    case class NewFile(file: File, timeAdded: Long)
    case class SourceAbandoned(uri: String)

  }

  class FileWatcher(source: String, logProcessor: ActorRef)
    extends Actor with FileWatchingCapabilities {

    import FileWatcher._

    register(source)

    override def receive: Receive = {
      case NewFile(file,_) =>
        logProcessor ! LogProcessor.LogFile(file)
      case SourceAbandoned(url)
        if (url==source) => self ! PoisonPill
    }

  }


  object LogProcessor {

    def props(dbWriter: ActorRef) : Props = Props(new LogProcessor(dbWriter))

    def name : String = s"log_processor_${UUID.randomUUID.toString}"

    //represents a new log file
    case class LogFile(file: File)
  }

  class LogProcessor(dbWriter: ActorRef) extends Actor with ActorLogging  with LogParsing {

    import LogProcessor._

    override def receive: Receive = {
      case LogFile(file) =>
        val lines : Vector[DbWriter.Line] = parse(file)
        lines.foreach(dbWriter ! _)
    }

  }

  object DbWriter {

    def props(databaseUrl: String) : Props = Props(new DbWriter(databaseUrl))

    def name(databaseUrl: String) =
      s"""db-writer-${databaseUrl.split("/").last}-${UUID.randomUUID.toString}"""

    case class Line(time: Long, message: String, messageType: String)

  }

  class DbWriter(databaseUrl: String) extends Actor {

    import DbWriter._

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
    import DbWriter._

    // Parses log files. creates line objects from the lines in the log file.
    // If the file is corrupt a CorruptedFileException is thrown
    // implement parser here, now just return dummy value
    def parse(file: File): Vector[Line] = { Vector.empty[Line]}

  }

  trait FileWatchingCapabilities {

    def register(uri: String): Unit = {}

  }


}


