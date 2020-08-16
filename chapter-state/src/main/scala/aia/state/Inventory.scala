package aia.state

import akka.actor.{Actor,ActorRef,FSM}

class Inventory(publisher: ActorRef) extends Actor
  with FSM[State,StateData] {

  var reservedId = 0
  startWith(WaitForRequests,new StateData(0,Seq()))

  when(WaitForRequests) {

    case Event(request: BookRequest, data: StateData) => {
      val newStateData  = data.copy(
        pendingRequests = data.pendingRequests :+ request)
      if (newStateData.nrBooksInStore > 0){
        goto(ProcessRequest) using newStateData
      } else {
        goto(WaitForPublisher) using newStateData
      }
    }

    case Event(PendingRequest, data: StateData) => {
      if (data.pendingRequests.isEmpty){
        stay
      } else if (data.nrBooksInStore > 0){
        goto(ProcessRequest)
      } else {
        goto(WaitForPublisher)
      }
    }

  }

  when(WaitForPublisher) {

    case Event(supply: BookSupply, data: StateData) => {
      goto(ProcessRequest) using data.copy(
        nrBooksInStore = supply.nrBooks
      )
    }

    case Event(BookSupplySoldOut, _) => {
      goto(ProcessSoldOut)
    }

  }

  when(ProcessRequest) {
    case Event(Done, data: StateData) => {
      goto(WaitForRequests) using data.copy(
        nrBooksInStore = data.nrBooksInStore - 1,
        pendingRequests = data.pendingRequests.tail
      )
    }
  }

  when(SoldOut) {
    case Event(request: BookRequest, data: StateData) => {
      goto(ProcessSoldOut) using new StateData(0,Seq(request))
    }
  }

  when(ProcessSoldOut) {
    case Event(Done, data: StateData) => {
      goto(SoldOut) using new StateData(0,Seq())
    }
  }

  whenUnhandled {
    // common code for all states
    case Event(request: BookRequest, data: StateData) => {
      stay using data.copy(
        pendingRequests = data.pendingRequests :+ request
      )
    }

    case Event(e, s) => {
      log.warning("received unhandled request {} in state {}/{}",e,stateName,s)
      stay
    }

  }

  initialize

  onTransition {

    case _ -> WaitForRequests => {
      if (!nextStateData.pendingRequests.isEmpty){
        self ! PendingRequest
      }
    }

    case _ -> WaitForPublisher => {
      // send request to publisher
      publisher ! PublisherRequest
    }


    case _ -> ProcessRequest => {
      val request = nextStateData.pendingRequests.head
      reservedId += 1
      request.target ! new BookReply(request.context,Right(reservedId))
      self ! Done
    }

    case _ -> ProcessSoldOut => {
      nextStateData.pendingRequests.foreach(request => {
        request.target ! new BookReply(request.context,Left("SoldOut"))
      })
      self ! Done
    }

  }

}
