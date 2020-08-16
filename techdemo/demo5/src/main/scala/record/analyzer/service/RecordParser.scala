package record.analyzer.service

import akka.http.scaladsl.model.{MediaType, MediaTypes}
import net.liftweb.json.{DefaultFormats, Serialization}

import scala.xml.XML
import record.analyzer.model.{DoneRecord, InvalidRecord, NormalRecord, Record}

trait RecordParser {

  implicit val formats = DefaultFormats

  def parse(headers: Map[String, Any], content: String): Record = {
    try {
      val contentType = MediaType.parse(headers.get("Content-Type").get.toString)
      contentType match {
        case Right(mediaType) => {
          mediaType match {
            case MediaTypes.`application/json` => {
              if (content.contains("done")) DoneRecord
              else if (content.contains("id")) Serialization.read[NormalRecord](content) else InvalidRecord(content)
            }
            case MediaTypes.`application/xml` => {
              val xml = XML.loadString(content)
              val msg = xml \\ "msg"
              val id = msg \\ "id"
              if (!id.isEmpty) NormalRecord("ok", (id \@ "value"))
              else if (!(msg \\ "done").isEmpty) DoneRecord else InvalidRecord(content)
            }
            case _ => InvalidRecord(content)
          }
        }
        case Left(_) => InvalidRecord(content)
      }
    } catch {
      case _: Exception => InvalidRecord(content)
    }
  }

}