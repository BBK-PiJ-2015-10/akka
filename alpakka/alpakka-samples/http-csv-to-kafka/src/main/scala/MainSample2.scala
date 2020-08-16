
import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, MediaRanges }
import akka.stream.scaladsl.{ Sink, Source }
import akka.util.ByteString

import scala.concurrent.Future


object MainSample2  extends  App {

  implicit val actorSystem =  ActorSystem("alpakka-samples")

  import actorSystem.dispatcher

  val uriAddress = "https://people.sc.fsu.edu/~jburkardt/data/csv/oscar_age_female.csv"
  val httpRequest = HttpRequest(uri = uriAddress)
    .withHeaders(Accept(MediaRanges.`text/*`))

  def extractEntityData(response: HttpResponse): Source[ByteString, _] =
    response match {
      case HttpResponse(OK, _, entity, _) => entity.dataBytes
      case notOkResponse =>
        Source.failed(new RuntimeException(s"illegal response $notOkResponse"))
    }

  val future: Future[Done] =
    Source
      .single(httpRequest) //: HttpRequest
      .mapAsync(1)(Http()singleRequest(_)) //: HttpResponse
      .flatMapConcat(extractEntityData) //: ByteString
      .runWith(Sink.foreach(println))

  future.onComplete { _ =>
    println("Done Again!")
    actorSystem.terminate()
  }

}
