package record.analyzer.service

//import akka.NotUsed
//import record.analyzer.model.ProcessedRecord
//import akka.actor.{Actor, ActorLogging, ActorRef, Props}
//import akka.stream.OverflowStrategy
//import akka.stream.scaladsl._
//import net.liftweb.json.Serialization
//import streamz.camel.StreamMessage

//https://doc.akka.io/docs/akka/current/stream/operators/Source/actorRef.html
// https://doc.akka.io/docs/akka/current/stream/actor-interop.html
//https://medium.com/vividcode/akka-streams-camel-integration-3b6073662e87
//https://doc.akka.io/docs/akka/2.5.4/scala/stream/stream-integrations.html

//TODO
  // Use https://doc.akka.io/docs/akka/current/stream/operators/Source/actorRef.html to create a sourceFromAnActor
  // Create a sink to dump data into the endpoint

object SinkHelper {

  //def props(destination: String) = Props(new SinkHelper(destination))

}

/*
class SinkHelper(destination: String) extends Actor with ActorLogging with RecordParser {

  val endpointUri: String = "http4:"+destination

  //val sink : Sink[StreamMessage[String],NotUsed]

  //override def receive: Receive = {
    //case processedRecord : ProcessedRecord => {

      //val source: Source[Any, ActorRef] = Source.actorRef[Any](bufferSize, OverflowStrategy.dropHead)
      //val actorRef: ActorRef = source.to(Sink.foreach(println)).run()
      //actorRef ! "hello"
      //actorRef ! "hello"


     // val source: Source[Any, ActorRef] = Source.actorRef[Any](10, OverflowStrategy.dropHead)
      //val target : ActorRef = source.via(send(endpointUri))
      //target ! processedRecord
      //val actorRef: ActorRef = source.to(Sink.foreach(println)).run()
      //val actorRef: ActorRef = sourc


      //val body : String = Serialization.write(processedRecord)
      //val s2: Source[StreamMessage[String],NotUsed] = ???
      //send

      //val sink : Sink[StreamMessage[String],NotUsed] = receive[String](endpointUri)

      //val ref =
        //Source.actorRef[String](10,OverflowStrategy.backpressure).to(sink)

      ///ref ! processedRecord

    }
  }

}

*/

