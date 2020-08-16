

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.camel.CamelExtension
import record.analyzer.api.RecordConsumerRest


object ApplicationRunner {

  def main(args: Array[String]): Unit = {

    val config = args.toSeq.iterator.next()
    startup(config)

  }

  def startup(configName: String): Unit = {

    val config = ConfigFactory.load(configName)

    val system = ActorSystem("records",config)

    val camel = CamelExtension(system)
    camel.context.start()

    if(configName.equals("chief")) {
      println("FUCK I am master monkeys")
      val camelUri : String =
        "jetty:http://localhost:8181/records"
      val consumer = system.actorOf(RecordConsumerRest.props(camelUri))

    }

    while(true){

    }

    println("Shutting off App")

  }


}
