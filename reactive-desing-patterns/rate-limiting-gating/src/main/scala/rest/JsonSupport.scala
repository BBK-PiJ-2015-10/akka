package rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import model._

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol{

  implicit val addFormat: RootJsonFormat[Add] = jsonFormat3(Add)

  implicit val addResultFormat: RootJsonFormat[AddResult] = jsonFormat1(AddResult)

  implicit val errorMsgFormat: RootJsonFormat[ErrorMessage] = jsonFormat1(ErrorMessage)

}
