package aia.state

import akka.actor.{ActorRef}

case class BookRequest(context: AnyRef, target: ActorRef)
case class BookSupply(nrBooks: Int)
case object BookSupplySoldOut
case object Done
case object PendingRequest


