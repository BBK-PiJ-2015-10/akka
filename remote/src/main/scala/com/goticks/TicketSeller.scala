package com.goticks

import akka.actor.{Actor,Props,PoisonPill}

object TicketSeller {
  def props(event: String) = Props(new TicketSeller(event))

  case class Ticket(id: Int)
  case class Tickets(event: String, entries : Vector[Ticket] = Vector.empty[Ticket])

  case class Add(tickets: Vector[Ticket])
  case class Buy(tickets: Int)
  case object GetEvent
  case object Cancel

}

class TicketSeller(event: String) extends Actor {
  import TicketSeller._

  var tickets = Vector.empty[Ticket]

  override def receive: Receive = {
    case Add(newTickets) => tickets ++ newTickets

    case Buy(nrOfTickets) =>
      val entries = tickets.take(nrOfTickets).toVector
      if (entries.size >= nrOfTickets){
        sender() !Tickets(event,entries)
        tickets = tickets.drop(nrOfTickets)
      } else sender() !Tickets(event)


    case GetEvent => sender() ! Some(BoxOffice.Event(event,tickets.size))

    case Cancel => sender() !Some(BoxOffice.Event(event,tickets.size))
                   self ! PoisonPill

  }

}
