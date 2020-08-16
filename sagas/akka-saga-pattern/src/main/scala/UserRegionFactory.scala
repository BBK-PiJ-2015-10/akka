import EmailRegionFactory.EmailRegion

import akka.actor.{ActorRef,ActorSystem,Props}
import akka.cluster.sharding.{ClusterSharding,ClusterShardingSettings}


object UserRegionFactory {
  type UserRegion = ActorRef

  def apply(system: ActorSystem, emailRegion: EmailRegion): UserRegion =
    ClusterSharding(system).start(
      typeName = "user",
      entityProps = Props(classOf[UserActor],emailRegion),
      settings = ClusterShardingSettings(system),
      extractEntityId= UserActor.idExtractor,
      extractShardId = UserActor.shardResolver
    )

}
