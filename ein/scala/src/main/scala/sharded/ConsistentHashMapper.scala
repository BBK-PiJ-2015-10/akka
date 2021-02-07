package sharded

import akka.routing.ConsistentHashingRouter.ConsistentHashMapping

object ConsistentHashMapper {

  def priceUpdateConsistentHashMapperByFirtLetter(): ConsistentHashMapping = {
    case msg: PriceUpdate => msg.companyName.substring(0,1)
  }

}
