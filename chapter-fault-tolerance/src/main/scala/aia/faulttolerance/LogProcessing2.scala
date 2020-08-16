package aia.faulttolerance

import java.io.File
import java.util.UUID


import language.postfixOps
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props, SupervisorStrategy, Terminated}
import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.OneForOneStrategy

import scala.concurrent.duration._


object LogProcessingApp2 extends App {


    val sources = Vector("file:///source1/","file:///source2/")
    val system = ActorSystem("logProcessing2")

    val databaseUrl = Vector("http://mydatabase1","http://mydatabase2","http://mydatabase3")

    system.actorOf(LogProcessingSupervisor.props(sources,databaseUrl),LogProcessingSupervisor.name)

  }



  object LogProcessingSupervisor {

    def props(sources: Vector[String], databaseUrls: Vector[String]) : Props = {
      Props(new LogProcessingSupervisor(sources,databaseUrls))
    }

    def name : String = "file-watcher-supervisor"

  }

  class LogProcessingSupervisor(sources: Vector[String], databaseUrls: Vector[String])
    extends Actor with ActorLogging {

    var fileWatchers : Vector[ActorRef] = sources.map { source =>
      val fileWatcher = context.actorOf(FileWatcher.props(source,databaseUrls))
      context.watch(fileWatcher)
      fileWatcher
    }

    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
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

    def props(source: String, dataBaseUrls: Vector[String]): Props = Props(new FileWatcher(source,dataBaseUrls))

    case class NewFile(file: File, timeAdded: Long)
    case class SourceAbandoned(uri: String)

  }

  class FileWatcher(source: String, dataBaseUrls: Vector[String])
    extends Actor with ActorLogging with FileWatchingCapabilities {

    import FileWatcher._

    register(source)


    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
      case _ : CorruptedFileException => Resume
    }

    val logProcessor : ActorRef = context.actorOf(Props(new LogProcessor(dataBaseUrls)),LogProcessor.name)
    context.watch(logProcessor)

    override def receive: Receive = {
      case NewFile(file,_) =>
        logProcessor ! LogProcessor.LogFile(file)
      case SourceAbandoned(uri)
        if (uri==source) =>
        log.info(s"$uri abandoned, stopping file watcher.")
        self ! PoisonPill
      case Terminated(`logProcessor`) =>
        log.info(s"Log processor terminated, stopping file watcher")
        self ! PoisonPill
    }

  }


  object LogProcessor {

    def props(databaseUrls: Vector[String]) : Props = Props(new LogProcessor(databaseUrls))

    def name : String = s"log_processor_${UUID.randomUUID.toString}"

    //represents a new log file
    case class LogFile(file: File)
  }

  class LogProcessor(dbBaseUrls: Vector[String]) extends Actor with ActorLogging  with LogParsing {
    import LogProcessor._

    require(dbBaseUrls.nonEmpty)

    val initialDatabaseUrl = dbBaseUrls.head
    var alternateDatabase = dbBaseUrls.tail


    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
      case _: DbBrokenConnectionException => Restart
      case _: DbNodeDownException => Stop
    }

    var dbWriter = context.actorOf(Props(new DbWriter(initialDatabaseUrl)),DbWriter.name(initialDatabaseUrl))
    context.watch(dbWriter)

    override def receive: Receive = {
      case LogFile(file) =>
        val lines : Vector[DbWriter.Line] = parse(file)
        lines.foreach(dbWriter ! _)
      case Terminated(_) =>
        if (alternateDatabase.nonEmpty){
          val newDatabaseUrl = alternateDatabase.head
          alternateDatabase = alternateDatabase.tail
          dbWriter = context.actorOf(Props(new DbWriter(newDatabaseUrl)),DbWriter.name(newDatabaseUrl))
          context.watch(dbWriter)
        } else {
          log.error("All Db notes broken, stopping")
          self ! PoisonPill
        }
    }

  }

  object DbWriter {

    def props(databaseUrl: String) : Props = Props(new DbWriter(databaseUrl))

    def name(databaseUrl: String) =
      s"""db-writer-${databaseUrl.split("/").last}"""

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


