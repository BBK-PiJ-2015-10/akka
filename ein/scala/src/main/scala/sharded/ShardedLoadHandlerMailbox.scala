package sharded

import akka.actor.ActorSystem
import akka.dispatch.{PriorityGenerator, UnboundedStablePriorityMailbox}
import com.typesafe.config.Config

class ShardedLoadHandlerMailbox(settings: ActorSystem.Settings, config: Config) extends
  UnboundedStablePriorityMailbox(
    PriorityGenerator {
      case Submit => 0
      case _: PriceUpdate => 1
    }
  )
