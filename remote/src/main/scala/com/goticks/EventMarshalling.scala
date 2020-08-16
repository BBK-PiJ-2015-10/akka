package com.goticks

import spray.json.DefaultJsonProtocol

case class EventDescription(tickets: Int){
  require(tickets > 0)
}

case class TicketRequest(tickets: Int){
  require(tickets > 0)
}

case class Error(message: String)


trait EventMarshalling extends DefaultJsonProtocol {
  import BoxOffice.{Event,Events}

  implicit val eventDescriptionFormat = jsonFormat1(EventDescription)
  implicit val eventFormat = jsonFormat2(Event)
  implicit val eventsFormat = jsonFormat1(Events)
  implicit val ticketRequestsFormat = jsonFormat1(TicketRequest)
  implicit val ticketFormat = jsonFormat1(TicketSeller.Ticket)
  implicit val ticketsFormat = jsonFormat2(TicketSeller.Tickets)
  implicit val errorFormat = jsonFormat1(Error)

}
