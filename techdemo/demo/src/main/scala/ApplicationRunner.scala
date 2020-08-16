

import akka.actor.{ActorSystem}
import akka.camel.{CamelExtension}
import akka.pattern.ask
import akka.util.Timeout
import org.apache.camel.spi.{Synchronization, UnitOfWork}
import org.apache.camel.{CamelContext, Endpoint, Exchange, ExchangePattern, Message}

import record.analyzer.state.{Coordinator}

object ApplicationRunner extends App{

  println("Starting App")
  val sourceAUrl = "localhost:7299/source/a"
  val sourceBUrl = "localhost:7299/source/b"
  val sinkAUrl = "localhost:7299/sink/a"

  val sources = Set(sourceAUrl,sourceBUrl)

  val system = ActorSystem()
  val camel = CamelExtension(system)
  camel.context.start()


  system.actorOf(Coordinator.props(sources,sinkAUrl))

  //implicit val timeOut = Timeout(10.seconds)
  //val sinkARef : ActorRef = system.actorOf(RecordSinkA.props(sinkAUrl))

  //val sourceAController = system.actorOf(SourceController.props())
  //val sourceBController = system.actorOf(SourceController.props())

  //val processor = system.actorOf(RecordProcessor.props())





  //sourceAController ! FetchRecords(sourceAUrl)
  //sourceBController ! FetchRecords(sourceBUrl)

  //helper ! FetchRecords(sourceAUrl)

  //val recordProcessorRef : ActorRef = system.actorOf(RecordProcessor.props());


  //val jsonSource = system.actorOf(Props(new RecordProducerJson(sourceAUrl)))
  //val xmlScource = system.actorOf(Props(new RecordProducerXml(sourceBUrl)))

  //val marc = Exchange.HTTP_METHOD
  //val value =

  //var headers = Map(Exchange.HTTP_METHOD ->"GET")
  //headers.
  //val message : CamelMessage = CamelMessage
  //message.apply("something",headers)

  //val order = Order("culon")

  //val exchange = Exchange.HTTP_METHOD
  //val value =
  //val resp = producer ! order
  //val resp2 = sourceARef ? "something"
  //val resp3 = sourceBRef ? "alexis"

  //println("here fucker",resp2)


  //val xmlConsumer = system.actorOf(Props(new RecordConsumer(sourceBUrl)))
  //camel.activationFutureFor(xmlConsumer)(5.seconds,system.dispatcher)

  while(true){

  }






  println("Shutting off App")

}
