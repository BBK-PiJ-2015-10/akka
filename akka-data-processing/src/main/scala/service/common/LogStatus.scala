package actors.common

import akka.routing.ConsistentHashingRouter.ConsistentHashable

case class LogStatus(value: String) extends ConsistentHashable {

  override def consistentHashKey: Any = value


}






