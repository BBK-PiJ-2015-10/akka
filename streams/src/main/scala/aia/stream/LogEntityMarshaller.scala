package aia.stream


import akka.util.ByteString

import akka.stream.scaladsl.{Source}

import akka.http.scaladsl.model.{ContentTypes,HttpEntity}
import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.marshalling.ToEntityMarshaller


object LogEntityMarshaller extends EventMarshalling {

  def create(maxJsonObject: Int): ToEntityMarshaller[Source[ByteString,_]] = {
    val js = ContentTypes.`application/json`
    val txt = ContentTypes.`text/plain(UTF-8)`

    val jsMarshaller = Marshaller.withFixedContentType(js) {
      src: Source[ByteString, _] => HttpEntity(js, src)
    }

    val txtMarshaller = Marshaller.withFixedContentType(txt) {
      src: Source[ByteString, _] => HttpEntity(txt, src.via(LogJson.jsonToLogFlow(maxJsonObject)))
    }

    Marshaller.oneOf(jsMarshaller, txtMarshaller)

  }


}
