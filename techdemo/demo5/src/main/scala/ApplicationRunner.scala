

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.camel.CamelExtension
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}
import record.analyzer.api.ApiRecordConsumerRest
import record.analyzer.service.{RecordSink, RecordSource}
import record.analyzer.service.sharded.{ShardedRecordSinks}
import record.analyzer.state.SourceController

//sbt "runMain ApplicationRunner seed" -Dport_number=2551
//sbt "runMain ApplicationRunner chief" -Dport_number=2552
//cd projects/tutorials/akka/techdemo/demo5
//python2 fixture.py -n 2000
//sbt "runMain ApplicationRunner bastard" -Dport_number=2553

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

    sourcesUrl.foreach(source =>
      system.actorOf(
        ClusterSingletonManager.props(
          SourceController.props(source),
          terminationMessage = "later",
          ClusterSingletonManagerSettings(system)
        ), name = "controller-"+source.substring(22)))

/*
    ClusterSharding(system)
      .start(
        ShardedRecordSinks.shardName,
        RecordSink.props(sinkUrl),
        ClusterShardingSettings(system),
        ShardedRecordSinks.extractEntityId,
        ShardedRecordSinks.extractShardId)
        */
    
    if(configName.equals("chief")) {
      println("I am the master monkeys")
      /*
      ClusterSharding(system)
        .startProxy(
          ShardedRecordSinks.shardName,
          Some("bastard"),
          None,
          ShardedRecordSinks.extractEntityId,
          ShardedRecordSinks.extractShardId)
      */
      system.actorOf(ApiRecordConsumerRest.props(sourcesUrl,sinkUrl))


    } else {
/*
      ClusterSharding(system)
        .start(
          ShardedRecordSinks.shardName,
          RecordSink.props(sinkUrl),
          ClusterShardingSettings(system),
          ShardedRecordSinks.extractEntityId,
          ShardedRecordSinks.extractShardId)

 */
    }


    while(true){

    }

    println("Shutting off App")

  }


}
