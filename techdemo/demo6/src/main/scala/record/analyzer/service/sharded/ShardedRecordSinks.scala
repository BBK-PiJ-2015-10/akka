package record.analyzer.service.sharded

import akka.cluster.sharding.{ShardRegion}
import record.analyzer.model.ProcessedRecord

object ShardedRecordSinks {

  val shardName : String = "sinks";

  val extractShardId : ShardRegion.ExtractShardId = {
    case msg: ProcessedRecord => math.abs(msg.id.hashCode % 5).toString
  }

  val extractEntityId : ShardRegion.ExtractEntityId = {
    case msg: ProcessedRecord => ((math.abs(msg.id.hashCode % 2).toString),msg)
  }

}
