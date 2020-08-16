package aia.stream

import java.nio.file.{Files, Path}
import java.nio.file.StandardOpenOption.{APPEND, CREATE, WRITE}

import akka.NotUsed
import akka.util.ByteString
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.collection.JavaConverters._
import scala.util.{Failure, Success}
import akka.stream.{ActorMaterializer, FlowShape, Graph, IOResult, OverflowStrategy, SourceShape}
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, GraphDSL, Keep, Merge, MergePreferred, Sink, Source}
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.common.EntityStreamingSupport
import spray.json._

class LogStreamProcessorApi(val logsDir: Path, val notificationsDir: Path, val metricsDir: Path, val maxLine: Int, val maxJsObject: Int )
                           (implicit  val materializer: ActorMaterializer, implicit val executionContext: ExecutionContext)
                            extends EventMarshalling {

  def logFile(logId: String): Path = logsDir.resolve(logId)
  def logStateFile(logId: String, state: State): Path = logFile(s"$logId-${State.norm(state)}")

  def logFileSource(logId: String) :Source[ByteString,Future[IOResult]]  = FileIO.fromPath(logFile(logId))
  def logFileSource(logId: String, state: State) : Source[ByteString,Future[IOResult]] = FileIO.fromPath(logStateFile(logId,state))

  def logFileSink(logId: String, state: State) : Sink[ByteString,Future[IOResult]] = FileIO.toPath(logStateFile(logId,state),Set(CREATE,APPEND,WRITE))
  def archiveSink(logId: String) : Sink[ByteString,Future[IOResult]] = FileIO.toPath(logFile(logId),Set(CREATE,WRITE,APPEND))

  val notificationsSink : Sink[ByteString,Future[IOResult]] = FileIO.toPath(notificationsDir.resolve("notifications.json"),Set(CREATE,WRITE,APPEND))
  val metricsSink : Sink[ByteString,Future[IOResult]]= FileIO.toPath(metricsDir.resolve("metrics.json"),Set(CREATE,WRITE,APPEND))

  implicit val unmarshaller = EventUnmarshaller.create(maxLine,maxJsObject)
  implicit val jsonStreamingSupport = EntityStreamingSupport.json()
  implicit val marshaller = LogEntityMarshaller.create(maxJsObject)

  def routes : Route = getRoute ~ getLogStateRoute ~ getLogsRoute ~ getLogNotOkRoute ~ deleteRoute ~ postRoute


  val StateSegment = Segment.flatMap{
    case State(state) => Some(state)
    case _ => None
  }


  def processEvents(logId: String): Graph[FlowShape[Event,ByteString],NotUsed] = {

    val jsFlow = LogJson.jsonOutFlow
    val notifyOutFlow = LogJson.notifyOutFlow
    val metricOutFlow = LogJson.metricOutFlow

    Flow.fromGraph(

      GraphDSL.create(){ implicit builder =>

        val nrWarnings = 100
        val nrErrors = 10
        val archBufSize = 100000
        val warnBufSize = 100
        val errBufSize = 1000
        val errDuration = FiniteDuration(10,TimeUnit.SECONDS)
        val warnDuration = FiniteDuration(1,TimeUnit.MINUTES)

        val toMetric = Flow[Event].collect {
          case Event(_, service, _, time, _ , Some(tag),Some(metric)) =>
            Metric(service,time,metric,tag)
        }

        val recordDrift = Flow[Metric].expand{ metric =>
            Iterator.from(0).map( d => metric.copy(drift = d))
        }

        val bcast = builder.add(Broadcast[Event](5))
        val wbcast = builder.add(Broadcast[Event](2))
        val ebcast = builder.add(Broadcast[Event](2))
        val cbcast = builder.add(Broadcast[Event](2))
        val okcast = builder.add(Broadcast[Event](2))

        val mergeNotify = builder.add(MergePreferred[Summary](2))
        val archive = builder.add(jsFlow)

        val toNot = Flow[Event].map(e => Summary(Vector(e)))

        val ok = Flow[Event].filter(_.state == Ok)
        val warning = Flow[Event].filter(_.state == Warning)
        val error = Flow[Event].filter(_.state == Error)
        val critical = Flow[Event].filter(_.state == Critical)

        def rollUp(nr: Int, duration: FiniteDuration) =
          Flow[Event].groupedWithin(nr,duration).map(events => Summary(events.toVector))

        val rollUpErr = rollUp(nrErrors,errDuration)
        val rollUpWarn = rollUp(nrWarnings,warnDuration)

        val archBuf = Flow[Event].buffer(archBufSize,OverflowStrategy.fail)
        val warnBuf = Flow[Event].buffer(warnBufSize,OverflowStrategy.dropHead)
        val errBuf = Flow[Event].buffer(errBufSize,OverflowStrategy.backpressure)
        val metricBuf = Flow[Event].buffer(errBufSize,OverflowStrategy.dropHead)


        bcast ~> archBuf ~> archive.in
        bcast ~> ok ~> okcast
        bcast ~> warning ~> wbcast
        bcast ~> error ~> ebcast
        bcast ~> critical ~> cbcast

        okcast ~> jsFlow ~> logFileSink(logId,Ok)
        okcast ~> metricBuf ~> toMetric ~> recordDrift ~> metricOutFlow ~> metricsSink

        cbcast ~> jsFlow ~> logFileSink(logId,Critical)
        cbcast ~> toNot ~> mergeNotify.preferred

        ebcast ~> jsFlow ~> logFileSink(logId,Error)
        ebcast ~> errBuf ~> rollUpErr ~> mergeNotify.in(0)

        wbcast ~> jsFlow ~> logFileSink(logId,Warning)
        wbcast ~> warnBuf ~> rollUpWarn ~> mergeNotify.in(1)

        mergeNotify ~> notifyOutFlow ~> notificationsSink

        FlowShape(bcast.in,archive.out)

      }

    )

  }

  def mergeNotOk(logId: String): Source[ByteString,NotUsed] = {
    val warning = logFileSource(logId,Warning).via(LogJson.jsonFramed(maxJsObject))
    val error = logFileSource(logId,Error).via(LogJson.jsonFramed(maxJsObject))
    val critical = logFileSource(logId,Critical).via(LogJson.jsonFramed(maxJsObject))

    Source.fromGraph(
      GraphDSL.create() { implicit builder =>

        val warningNode = builder.add(warning)
        val errorNode = builder.add(error)
        val criticalNode =  builder.add(critical)
        val merge = builder.add(Merge[ByteString](3))

        warningNode ~> merge
        errorNode ~> merge
        criticalNode ~> merge

        SourceShape(merge.out)

      })
  }

  def postRoute =
    pathPrefix("logs" / Segment){ logId => {
      pathEndOrSingleSlash {
        post {
          entity(asSourceOf[Event]){ src =>
            onComplete(
              src.via(processEvents(logId)).toMat(archiveSink(logId))(Keep.right).run()
            ){
              case Success(IOResult(count, status)) => complete((StatusCodes.OK),LogReceipt(logId,count))
              case Success(IOResult(count,Failure(e))) => complete((StatusCodes.BadRequest),ParseError(logId,e.getMessage))
              case Failure(e) => complete((StatusCodes.BadRequest),ParseError(logId,e.getMessage))
            }
          }
        }
      }
    }
    }

  def getRoute =
    pathPrefix("logs" / Segment){ logId => {
      pathEndOrSingleSlash {
        get {
          extractRequest { req =>
            if (Files.exists(logFile(logId))){
              val src = logFileSource(logId)
              complete(Marshal(src).toResponseFor(req))
            } else {
              complete(StatusCodes.NotFound)
            }
          }
        }
      }
    }
    }

  def getLogStateRoute =
    pathPrefix("logs" / Segment / StateSegment){(logId,state) =>
      pathSingleSlash {
        get {
          extractRequest { req =>
            if (Files.exists(logStateFile(logId,state))){
              val src = logFileSource(logId,state)
              complete(Marshal(src).toResponseFor(req))
            } else {
              complete(StatusCodes.NotFound)
            }
          }
        }
      }
    }

  def mergeSources[E](sources: Vector[Source[E,_]]): Option[Source[E,_]] = {
    if (sources.size == 0) None
    else if (sources.size == 1) Some(sources(0))
    else {
      Some(Source.combine(
        sources(0),
        sources(1),
        sources.drop(2) : _*
      )(Merge(_)))
    }
  }

  def getFileSources[T](dir: Path):Vector[Source[ByteString,Future[IOResult]]] ={
    val dirStream = Files.newDirectoryStream(dir)
    try {
      val paths = dirStream.iterator().asScala.toVector
      paths.map(path => FileIO.fromPath(path)).toVector
    } finally {
      dirStream.close()
    }
  }

  def getLogsRoute =
    pathPrefix("logs"){
      pathEndOrSingleSlash {
        get {
          extractRequest { req =>
            val sources = getFileSources(logsDir).map { src =>
              src.via(LogJson.jsonFramed(maxJsObject))
            }
            mergeSources(sources) match {
              case Some(src) => complete(Marshal(src).toResponseFor(req))
              case None => complete(StatusCodes.NotFound)
            }
          }
        }
      }
    }

  def deleteRoute =
    pathPrefix("logs" / Segment){ logId => {
      pathEndOrSingleSlash {
        delete {
          if (Files.deleteIfExists(logFile(logId))){
            complete(StatusCodes.OK)
          } else {
            complete(StatusCodes.NotFound)
          }
        }
      }
    }
    }

  def getLogNotOkRoute =
    pathPrefix("logs" / Segment / "not-ok") { logId =>
      pathEndOrSingleSlash {
        get {
          extractRequest { req =>
            complete(Marshal(mergeNotOk(logId)).toResponseFor(req))
          }
        }
      }
    }



}