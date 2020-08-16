

import akka.Done
import akka.actor.{ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, MediaRanges}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString

import scala.concurrent.Future

//https://github.com/akka/alpakka-samples/tree/master/alpakka-sample-http-csv-to-kafka
////https://akka.io/alpakka-samples/http-csv-to-kafka/index.html#fetch-csv-via-akka-http-and-publish-the-data-as-json-to-kafka

object MainSample1 extends App{

  implicit val actorSystem = ActorSystem("alpakka-samples")

  import actorSystem.dispatcher

  val uriAddress = "https://people.sc.fsu.edu/~jburkardt/data/csv/oscar_age_female.csv"
  val httpRequest = HttpRequest(uri = uriAddress)
  //val httpRequest = HttpRequest(uri = uriAddress)
  .withHeaders(Accept(MediaRanges.`text/*`))

  def extractEntityData(response: HttpResponse) : Source[ByteString,_] =
    response match {
      case HttpResponse(OK,_,entity,_) => entity.dataBytes
      case notOkResponse => Source.failed(new RuntimeException(s"Illegal response $notOkResponse"))
    }

  val future : Future[Done] = Source
    .single(httpRequest)
    .mapAsync(1)(Http().singleRequest(_))
    .runWith(Sink.foreach(println))

  future.map { _ =>
    println("Done Baltimora")
    actorSystem.terminate()
  }


}
