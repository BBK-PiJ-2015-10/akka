package com.goticks

import scala.concurrent.duration
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import akka.actor.{ActorRef}
import akka.event.LoggingAdapter
import akka.pattern.ask
import akka.util.Timeout

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{StatusCodes}
import akka.http.scaladsl.server.Directives._
//import akka.http.scaladsl.server.Directives.{pathPrefix,Segment,pathEndOrSingleSlash,get,post,delete,entity,onSuccess,complete,as,/}
import akka.http.scaladsl.server.{Route}


trait RestApi extends BoxOfficeApi with EventMarshalling {
  import StatusCodes.{OK,NotFound,Created,BadRequest}
  import BoxOffice.{CreateEvent,EventResponse,GetEvents,GetEvent,Event,Events,CancelEvent,GetTickets,EventCreated,EventExists}

  def routes: Route = eventsRoute ~ ticketsRoute ~ eventRoute

  def eventsRoute =
    pathPrefix("events"){
      pathEndOrSingleSlash {
        get {
          // GET / events
          onSuccess(getEvents()){ events=>
            complete(OK,events)
          }
        }
      }
    }

  def eventRoute =
    pathPrefix("events"/Segment){ event =>
      pathEndOrSingleSlash {
        post {
          // POST /events/:event
          entity(as[EventDescription]){ed =>
            onSuccess(createEvent(event,ed.tickets)){
              case BoxOffice.EventCreated(event) => complete(Created,event)
              case BoxOffice.EventExists =>
                val err = Error(s"$event event exists already.")
                complete(BadRequest,err)
            }

          }
        }
        get {
          //GET /events/:event
          onSuccess(getEvent(event)){
            _.fold(complete(NotFound))(e => complete(OK,e))
          }
        }
        delete {
          // DELETE /events/:event
          onSuccess(cancelEvent(event)){
            _.fold(complete(NotFound))(e => complete(OK,e))
          }
        }
      }
    }

  def ticketsRoute  =
    pathPrefix("events"/Segment / "tickets"){ event =>
      post {
        pathEndOrSingleSlash {
          // POST / events/:event/tickets
          entity(as[TicketRequest]){ request =>
            onSuccess(requestTickets(event,request.tickets)){tickets=>
              if (tickets.entries.isEmpty) complete(NotFound)
              else complete(Created,tickets)
            }
          }
        }
      }
    }

}


trait BoxOfficeApi {
  import BoxOffice.{CreateEvent,EventResponse,GetEvents,GetEvent,Event,Events,CancelEvent,GetTickets,EventCreated,EventExists}

  def log : LoggingAdapter
  def createBoxOffice() : ActorRef

  implicit def executionContext : ExecutionContext
  implicit def requestTimeout : Timeout

  lazy val boxOffice = createBoxOffice()

  def createEvent(event: String, nrOfTickets : Int) = {
    log.info(s"Received new event $event, sending to $boxOffice")
    boxOffice.ask(CreateEvent(event,nrOfTickets)).mapTo[EventResponse]
  }

  def getEvents() = boxOffice.ask(GetEvents).mapTo[Events]

  def getEvent(event: String) = boxOffice.ask(GetEvent(event)).mapTo[Option[Event]]

  def cancelEvent(event: String) = boxOffice.ask(CancelEvent(event)).mapTo[Option[Event]]

  def requestTickets(event: String, tickets: Int) = boxOffice.ask(GetTickets(event,tickets)).mapTo[TicketSeller.Tickets]


}

