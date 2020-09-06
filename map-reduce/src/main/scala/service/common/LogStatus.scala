package service.common

import akka.routing.ConsistentHashingRouter.ConsistentHashable

case class LogStatus(value: String)

//case class LogStatus(value: String) extends ConsistentHashable {

  //override def consistentHashKey: Any = Integer.valueOf(value) % 5

  //override def consistentHashKey: Any = value








