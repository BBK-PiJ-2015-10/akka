package aia.stream

import scala.concurrent.{ExecutionContext, Future}

import akka.stream.scaladsl.{Source}
import akka.stream.{Materializer}


import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.http.scaladsl.unmarshalling.Unmarshaller._
import akka.http.scaladsl.model.{ContentTypeRange, ContentTypes,HttpEntity}

object EventUnmarshaller extends EventMarshalling {

  val supported = Set[ContentTypeRange](
    ContentTypes.`text/plain(UTF-8)`,
    ContentTypes.`application/json`
  )


  def create(maxLine: Int, maxJsonObject: Int) = {
    new Unmarshaller[HttpEntity, Source[Event, _]] {
      override def apply(entity: HttpEntity)(implicit ec: ExecutionContext,
                                             materializer: Materializer): Future[Source[Event, _]] = {

        val future = entity.contentType match {
          case ContentTypes.`text/plain(UTF-8)` => Future.successful(LogJson.textInFlow(maxLine))
          case ContentTypes.`application/json` => Future.successful(LogJson.jsonInFlow(maxJsonObject))
          case other => Future.failed(new UnsupportedContentTypeException(supported))
        }
        future.map(flow => entity.dataBytes.via(flow))(ec)
      }
    }.forContentTypes(supported.toList: _*)

  }

}
