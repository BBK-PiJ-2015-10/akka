import akka.actor.{ActorRef,ActorSystem,Props}
import akka.cluster.sharding.{ClusterSharding,ClusterShardingSettings}

object EmailRegionFactory {

  type EmailRegion = ActorRef

  def apply(system: ActorSystem): EmailRegion =
    ClusterSharding(system).start(
      typeName =  "email",
      entityProps = Props[EmailActor],
      extractEntityId = EmailActor.idExtractor,
      extractShardId = EmailActor.shardResolver
    )

}
