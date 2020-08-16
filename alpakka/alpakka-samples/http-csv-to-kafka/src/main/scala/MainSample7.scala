
import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, MediaRanges}
import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}
import akka.stream.scaladsl.{Sink, Source,Keep}
import akka.util.ByteString
import spray.json.{DefaultJsonProtocol, JsValue, JsonWriter}

import akka.kafka.scaladsl.{Consumer,Producer}
import akka.kafka.{ConsumerSettings,ProducerSettings,Subscriptions}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.{ProducerRecord}
import org.apache.kafka.common.serialization.{StringSerializer,StringDeserializer}

import scala.concurrent.Future

//https://www.programcreek.com/scala/akka.kafka.ProducerSettings

object MainSample7  extends  App with DefaultJsonProtocol {

  implicit val actorSystem =  ActorSystem[Nothing](Behaviors.empty, "alpakka-samples")

  import actorSystem.executionContext

  val uriAddress = "https://people.sc.fsu.edu/~jburkardt/data/csv/oscar_age_female.csv"
  val httpRequest = HttpRequest(uri = uriAddress)
    .withHeaders(Accept(MediaRanges.`text/*`))

  def extractEntityData(response: HttpResponse): Source[ByteString, _] =
    response match {
      case HttpResponse(OK, _, entity, _) => entity.dataBytes
      case notOkResponse =>
        Source.failed(new RuntimeException(s"illegal response $notOkResponse"))
    }

  def cleanseCsvData(csvData: Map[String,ByteString]) =
    csvData
      .filterNot { case(key, _) => key.isEmpty}
      .view
      .mapValues(_.utf8String)
      .toMap

  def toJson(map: Map[String, String])(
    implicit jsWriter: JsonWriter[Map[String, String]]): JsValue = jsWriter.write(map)

  val kafkaProducerSettings = ProducerSettings(actorSystem.toClassic,new StringSerializer,new StringSerializer)
    .withBootstrapServers("localhost:9092")

  val future: Future[Done] =
    Source
      .single(httpRequest) //: HttpRequest
      .mapAsync(1)(Http()(actorSystem.toClassic).singleRequest(_)) //: HttpResponse
      .flatMapConcat(extractEntityData) //: ByteString
      .via(CsvParsing.lineScanner())
      .via(CsvToMap.toMap())
      .map(cleanseCsvData) //: Map[String, String]
      .map(toJson) //: JsValue
      .map(_.compactPrint)
      .map{ elem =>
        new ProducerRecord[String,String]("topic1",elem)
      }
      .runWith(Producer.plainSink(kafkaProducerSettings))

  val cs = CoordinatedShutdown(actorSystem)
  cs.addTask(CoordinatedShutdown.PhaseServiceStop,"shut-down-client-http-pool")(() =>
    Http()(actorSystem.toClassic).shutdownAllConnectionPools().map(_ => Done)
  )

  val kafkaConsumerSettings = ConsumerSettings(actorSystem.toClassic,new StringDeserializer,new StringDeserializer)
    .withBootstrapServers("localhost:9092")
    .withGroupId("topic1")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest")

  val consumer = Consumer
    .atMostOnceSource(kafkaConsumerSettings,Subscriptions.topics("topic1"))
    .map(_.value)
    .toMat(Sink.foreach(println))(Keep.both)
    .mapMaterializedValue(Consumer.DrainingControl.apply)
    .run()

  for {
    _ <- future
    _ <- consumer.drainAndShutdown()
  }{
    cs.run(CoordinatedShutdown.UnknownReason)
  }

}
