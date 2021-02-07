import akka.actor.{ActorSystem}
import akka.dispatch.{PriorityGenerator, UnboundedStablePriorityMailbox}
import com.typesafe.config.Config

class SimpleLoadHandlerMailbox(settings: ActorSystem.Settings, config: Config) extends
  UnboundedStablePriorityMailbox(
    PriorityGenerator {
      case Submit => 0
      case priceUpdate: PriceUpdate => 1
    }
  )
