

import java.io.StringWriter
import java.time.Instant

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.event.{Logging, LoggingAdapter}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscription, Subscriptions}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS, MqttSubscriptions}
import akka.stream.alpakka.mqtt.scaladsl.{MqttSink, MqttSource}
import akka.stream.scaladsl.{Keep, RestartSource, Sink, Source}
import akka.stream.{KillSwitches, UniqueKillSwitch}
import akka.util.ByteString
import com.fasterxml.jackson.annotation.{JsonCreator, JsonProperty}
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.{ObjectMapper, ObjectWriter}
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.slf4j.LoggerFactory
//import org.testcontainers.containers.KafkaContainer

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.jdk.CollectionConverters._

// Source: https://github.com/akka/alpakka-samples/blob/master/alpakka-sample-mqtt-to-kafka/src/main/scala/samples/scaladsl/Main.scala
/*
Run with local Kafka
bin/zookeeper-servestart.sh config/zookeeper.properties
bin/kafka-server-start.sh config/server.properties
bin/kafka-topics.sh --create --topic measurements --replication-factor 1 --partitions 1 --zookeeper localhost:2181
bin/kafka-console-consumer.sh --topic measurements --bootstrap-server localhost:9092 --from-beginning
bin/kafka-console-producer.sh --topic measurements --broker-list localhost:9092
 */
object Main extends App {

  //val kafkaContainer = new KafkaContainer()

  //kafkaContainer.start()

  //try {
    val me = new Main();
    //me.run(kafkaContainer.getBootstrapServers)
    me.run("localhost:9092")
  //} //finally kafkaContainer.stop()

  final case class Measurement(timestamp: Instant, level: Long)

}

class Main {

  implicit val system = ActorSystem(Behaviors.empty,"MqttToKafka")
  implicit val ec: ExecutionContext = system.executionContext
  val log = LoggerFactory.getLogger(classOf[Main])

  // #json-mechanics
  val jsonFactory = new JsonFactory
  val mapper = new ObjectMapper()
    .registerModule(new JavaTimeModule)
    .registerModule(com.fasterxml.jackson.module.scala.DefaultScalaModule)

  val measurementReader = mapper.readerFor(classOf[Main.Measurement])
  val measurementWriter = mapper.writerFor(classOf[Main.Measurement])


  private def asJsonArray(fieldName: String, list:Seq[AnyRef]) = {
    val sw = new StringWriter
    val generator = jsonFactory.createGenerator(sw)
    generator.writeStartObject()
    generator.writeFieldName(fieldName)
    measurementWriter.writeValues(generator).init(true).writeAll(list.asJava)
    generator.close()
    sw.toString
  }

  // #restarting
  /**
   * Wrap a source with restart logic and expose an equivalent materialized value.
   */
  private def wrapWithAsRestartSource[M](source: Source[M,Future[Done]]): Source[M,Future[Done]] = {
    val fut = Promise[Done]
    RestartSource.withBackoff(100.millis,3.seconds,randomFactor = 0.2d, maxRestarts = 5) {
      () => source.mapMaterializedValue(mat => fut.completeWith(mat))
    }.mapMaterializedValue(_ => fut.future)
  }
  // #restarting

  private def run(kafkaServer: String): Unit = {

    implicit val logAdapter : LoggingAdapter = Logging.getLogger(system.toClassic,getClass.getName)
    // # flow
    val connectionSettings = MqttConnectionSettings("tcp://localhost:1883","coffee-client", new MemoryPersistence)

    val topic = "coffee"

    val subscription = MqttSubscriptions.create(topic,MqttQoS.AtLeastOnce)

    val restartingMqttSource =
      wrapWithAsRestartSource(MqttSource.atMostOnce(connectionSettings.withClientId("coffee-control"),subscription,8)
    )

    //Set up Kafka producer Sink
    val producerSettings =
      ProducerSettings(system.toClassic,new StringSerializer,new StringSerializer)
      .withBootstrapServers(kafkaServer)
    val kafkaProducer = Producer.plainSink(producerSettings)
    val kafkaTopic = "measurement"

    val ((subscriptionInitialized, listener),streamCompletion) =
      restartingMqttSource.viaMat(KillSwitches.single)(Keep.both)
      .map(_.payload.utf8String)
      .map(measurementReader.readValue)
      .groupedWithin(50,5.seconds)
      .map(list => asJsonArray("measurement",list))
      .log("producing to Kafka")
      .map(json => new ProducerRecord[String,String](kafkaTopic,"",json))
      .toMat(kafkaProducer)(Keep.both)
      .run


    // start producing messages to MQTT
    val producer = subscriptionInitialized.map(_ => produceMessages(connectionSettings,topic))
    streamCompletion
      .recover {
        case exception =>
          exception.printStackTrace()
          null
      }
      .foreach(_ => system.terminate)


    //

    val kafkaConsumerSettings = ConsumerSettings(system.toClassic,new StringDeserializer,new StringDeserializer)
      .withBootstrapServers("localhost:9092")
      .withGroupId("culon")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest")

    val consumer = Consumer
      .atMostOnceSource(kafkaConsumerSettings,Subscriptions.topics(kafkaTopic))
      .map(_.value)
      .toMat(Sink.foreach(println))(Keep.both)
      //.mapMaterializedValue(Consumer.DrainingControl.apply)
      .run()

    log.info("Letting things run for a while")
    Thread.sleep(60 * 1000)
    producer.foreach(_.shutdown)
    listener.shutdown()

  }


  /**
   * Simulate messages from MQTT by writing to topic registered in MQTT broker.
   */
  private def produceMessages(connectionSettings: MqttConnectionSettings, topic: String) : UniqueKillSwitch = {

    import Main.Measurement

    val input = Seq(
    Measurement(Instant.now,40),
    Measurement(Instant.now,60),
    Measurement(Instant.now, 80),
    Measurement(Instant.now, 100),
    Measurement(Instant.now, 120)
    )

    val sinkSettings = connectionSettings.withClientId("coffee-supervisor")
    val killSwitch = Source
    .cycle(() => input.iterator)
    .throttle(4,1.second)
    .map(measurementWriter.writeValueAsString)
    .map((s: String) => MqttMessage.create(topic,ByteString.fromString(s)))
    .viaMat(KillSwitches.single)(Keep.right)
    .toMat(MqttSink(sinkSettings,MqttQoS.AtLeastOnce))(Keep.left)
    .run()

    killSwitch

  }


}
