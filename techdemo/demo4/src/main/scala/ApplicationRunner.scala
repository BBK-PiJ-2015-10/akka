

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.camel.CamelExtension
import record.analyzer.api.ApiRecordConsumerRest

//sbt "runMain ApplicationRunner application" -Dport_number=2551
//cd projects/tutorials/akka/techdemo/demo4
//python2 fixture.py -n 2000


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

    val sourceAUrl = "localhost:7299/source/a"
    val sourceBUrl = "localhost:7299/source/b"
    val sinkUrl = "localhost:7299/sink/a"
    val sourcesUrl = Set(sourceAUrl,sourceBUrl)

    system.actorOf(ApiRecordConsumerRest.props(sourcesUrl,sinkUrl))

    while(true){

    }

    println("Shutting off App")

  }


}
