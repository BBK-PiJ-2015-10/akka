package aia.stream

import java.nio.file.{Files, Path, Paths}
import java.nio.file.StandardOpenOption.{CREATE,APPEND,WRITE}

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.{Success,Failure}
import scala.collection.JavaConverters._

import akka.stream.{ActorMaterializer, FlowShape, Graph, IOResult, SourceShape}
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source,Keep}
import akka.stream.scaladsl.GraphDSL.Implicits._

import akka.util.ByteString
import akka.{NotUsed,Done}

import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

import spray.json._

class FanLogsApi(val logsDir: Path, val maxLine: Int, val maxJsObject : Int)
                (implicit val executionContext: ExecutionContext, implicit val materializer: ActorMaterializer)
                extends EventMarshalling {

  def logFile(id: String): Path = logsDir.resolve(id)

  def logStateFile(logId: String, state: State): Path = logFile(s"$logId-${State.norm(state)}")

  def logFileSink(logId: String, state: State): Sink[ByteString, Future[IOResult]] = FileIO.toPath(logStateFile(logId, state))

  def logFileSink(logId: String): Sink[ByteString, Future[IOResult]] = FileIO.toPath(logFile(logId), Set(CREATE, APPEND, WRITE))

  def logFileSource(logId: String, state: State): Source[ByteString, Future[IOResult]] = FileIO.fromPath(logStateFile(logId, state))

  def logFileSource(logId: String): Source[ByteString, Future[IOResult]] = FileIO.fromPath(logFile(logId))

  def processStates(logId: String): Graph[FlowShape[Event, ByteString], NotUsed] = {
    val jsFlow = LogJson.jsonOutFlow

    Flow.fromGraph(
      GraphDSL.create() { implicit builder =>

        val bcast = builder.add(Broadcast[Event](5))
        val js = builder.add(jsFlow)

        val ok = Flow[Event].filter(_.state == Ok)
        val warning = Flow[Event].filter(_.state == Warning)
        val error = Flow[Event].filter(_.state == Error)
        val critical = Flow[Event].filter(_.state == Critical)

        bcast ~> js.in
        bcast ~> ok ~> jsFlow ~> logFileSink(logId, Ok)
        bcast ~> warning ~> jsFlow ~> logFileSink(logId, Warning)
        bcast ~> error ~> jsFlow ~> logFileSink(logId, Error)
        bcast ~> critical ~> jsFlow ~> logFileSink(logId, Critical)

        FlowShape(bcast.in, js.out)

      })
  }

  def mergeNotOk(logId: String): Source[ByteString, NotUsed] = {

    val warning = logFileSource(logId, Warning).via(LogJson.jsonFramed(maxJsObject))

    val error = logFileSource(logId, Error).via(LogJson.jsonFramed(maxJsObject))

    val critical = logFileSource(logId, Critical).via(LogJson.jsonFramed(maxJsObject))

    Source.fromGraph(
      GraphDSL.create() { implicit builder =>

        val warningShape = builder.add(warning)
        val errorShape = builder.add(error)
        val criticalShape = builder.add(critical)
        val merge = builder.add(Merge[ByteString](3))

        warningShape ~> merge
        errorShape ~> merge
        criticalShape ~> merge

        SourceShape(merge.out)

      }
    )

  }

  def routes: Route = getRoute ~
                      postRoute ~
                      deleteRoute ~
                      getLogStateRoute ~
                      getLogsRoute ~
                      getLogNotOkRoute

  implicit val unmarshaller = EventUnmarshaller.create(maxLine, maxJsObject)

  implicit val jsonStreamingSupport = EntityStreamingSupport.json()

  implicit val marshaller = LogEntityMarshaller.create(maxJsObject)

  def postRoute =
    pathPrefix("logs" / Segment) { logId =>
      pathEndOrSingleSlash {
        post {
          entity(asSourceOf[Event]) { src =>
            onComplete(
              src.via(processStates(logId))
                .toMat(logFileSink(logId))(Keep.right)
                .run()
            ) {
              case Success(IOResult(count, Success(Done))) =>
                complete((StatusCodes.OK, LogReceipt(logId, count)))
              case Success(IOResult(count, Failure(e))) =>
                complete((StatusCodes.BadRequest, ParseError(logId, e.getMessage)))
              case Failure(e) => complete((StatusCodes.BadRequest, ParseError(logId, e.getMessage)))
            }
          }
        }
      }
    }


  def getRoute =
    pathPrefix("logs" / Segment) { logId =>
      pathEndOrSingleSlash {
        get {
          extractRequest { req =>
            if (Files.exists(logFile(logId))) {
              val src = logFileSource(logId)
              complete(
                Marshal(src).toResponseFor(req)
              )
            } else {
              complete(StatusCodes.NotFound)
            }
          }
        }
      }
    }


  val StateSegment = Segment.flatMap {
    case State(state) => Some(state)
    case _ => None
  }

  def getLogStateRoute =
    pathPrefix("logs" / Segment / StateSegment) { (logId, state) =>
      pathEndOrSingleSlash {
        get {
          extractRequest { req =>
            if (Files.exists(logStateFile(logId,state))) {
              val src = logFileSource(logId,state)
              complete(Marshal(src).toResponseFor(req))
            } else {
              complete(StatusCodes.NotFound)
            }
          }
        }
      }
    }

  def mergeSources[E](sources: Vector[Source[E, _]]): Option[Source[E, _]] = {
    if (sources.size == 0) None
    else if (sources.size == 1) Some(sources(0))
    else {
      Some(Source.combine(
        sources(0),
        sources(1),
        sources.drop(2): _*
      )(Merge(_)))
    }
  }


  def getFilesSources[T](dir: Path): Vector[Source[ByteString,Future[IOResult]]] = {
    val dirStream = Files.newDirectoryStream(dir)
    try {
      val paths = dirStream.iterator().asScala.toVector
      paths.map(path => FileIO.fromPath(path)).toVector
    } finally dirStream.close()
  }

  def getLogsRoute =
    pathPrefix("logs"){
      pathEndOrSingleSlash {
        get {
          extractRequest { req =>
            val sources = getFilesSources(logsDir).map { src =>
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
          }
          else {
            complete(StatusCodes.NotFound)
          }
        }
      }
    }
    }

  def getLogNotOkRoute =
    pathPrefix("logs" / Segment){ logId =>
      pathEndOrSingleSlash {
        get {
          extractRequest { req =>
            complete(Marshal(mergeNotOk(logId)).toResponseFor(req))
          }
        }
      }
    }

}
