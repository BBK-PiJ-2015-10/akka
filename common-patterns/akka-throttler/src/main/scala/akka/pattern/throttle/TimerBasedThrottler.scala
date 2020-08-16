package akka.pattern.throttle

import akka.actor.{Actor, ActorRef,FSM,LoggingFSM}
import akka.pattern.throttle.Throttler._
import akka.pattern.throttle.TimerBasedThrottler._

import scala.collection.immutable.{Queue => Q}
import scala.util.control.NonFatal

//Credit: https://github.com/hbf/akka-throttler/blob/master/src/main/scala/akka/pattern/throttle/TimerBasedThrottler.scala

object TimerBasedThrottler {

  case object Tick

  sealed trait TState

  case object Idle extends TState

  case object Active extends TState

  case class Message(message: Any, sender: ActorRef)

  sealed case class Data(target:Option[ActorRef], callsLeftInThisPeriod: Int, queue: Q[Message])

}


import TimerBasedThrottler._
import Throttler._

class TimerBasedThrottler(var rate: Rate) extends Actor with LoggingFSM[TState, Data] {

  startWith(Idle,Data(None,rate.numberOfCalls,Q[Message]()))

  when(Idle){

    // Set the rate
    case Event(SetRate(rate), d) => {
      this.rate = rate
      stay using d.copy(callsLeftInThisPeriod = rate.numberOfCalls)
    }

    //Set the target
    case Event(SetTarget(t @Some(_)),d) if !d.queue.isEmpty => {
      goto(Active) using deliverMessages(d.copy(target =t))
    }
    case Event(SetTarget(t),d) => stay using d.copy(target = t)


    case Event(Queue(msg), d @ Data(None, _, queue)) => {
      stay using d.copy(queue = queue.enqueue(Message(msg,context.sender())))
    }
    case Event(Queue(msg), d @ Data(Some(_),_,Seq())) => {
      goto(Active) using deliverMessages(d.copy(queue = Q(Message(msg,context.sender()))))
    }

  }

  when (Active) {

    case Event(SetRate(rate),d) => {
      this.rate = rate
      stopTimer()
      startTimer(rate)
      stay() using d.copy(callsLeftInThisPeriod = rate.numberOfCalls)
    }

    case Event(SetTarget(None), d) => {
      goto(Idle) using d.copy(target = None)
    }

    case Event(SetTarget(t @Some(_)),d) => {
      stay using d.copy(target = t)
    }

    case Event(Queue(msg), d @Data(_,0,queue)) => {
      stay using d.copy(queue = queue.enqueue(Message(msg,context.sender())))
    }

    case Event(Queue(msg), d @Data(_,_,queue)) => {
      stay using deliverMessages(d.copy(queue = queue.enqueue(Message(msg,context.sender()))))
    }

    case Event(Tick, d @Data(_,_,Seq())) => {
      goto(Idle)
    }

    // Period ends and we get more occasions to send messages
    case Event(Tick,d @Data(_,_,_)) => {
      stay using deliverMessages(d.copy(callsLeftInThisPeriod = rate.numberOfCalls))
    }

  }

  onTransition {
    case Idle -> Active => startTimer(rate)
    case Active -> Idle => stopTimer()
  }


  initialize()

  private def startTimer(rate: Rate) = setTimer("morePermits",Tick,rate.duration,true)
  private def stopTimer() = cancelTimer("morePermits")


  private def deliverMessages(data: Data): Data = {
    val queue = data.queue
    val nrOfMessagesToSend = scala.math.min(queue.length,data.callsLeftInThisPeriod)
    queue.take(nrOfMessagesToSend).foreach((x: Message) => {
      try {
        data.target.get tell(x.message,x.sender)
      } catch {
        case NonFatal(ex) => throw new FailedToSendException("tell() failed.",ex)
      }
    })
    data.copy(queue = queue.drop(nrOfMessagesToSend),callsLeftInThisPeriod = data.callsLeftInThisPeriod - nrOfMessagesToSend)
  }

}


/*
class TimerBasedThrottler(var rate: Rate) extends Actor with Throtler with FSM[TState,Data] {

  startWith(Idle,Data(None,rate.numberOfCalls,Q[Message]()))

  when(Idle){

    // Set the rate
    case Event(SetRate(rate), d) => {
       this.rate = rate
       stay using d.copy(callsLeftInThisPeriod = rate.numberOfCalls)
    }

    //Set the target
    case Event(SetTarget(t @Some(_)),d) if !d.queue.isEmpty => {
        goto(Active) using deliverMessages(d.copy(target =t))
    }
    case Event(SetTarget(t),d) => stay using d.copy(target = t)


    case Event(Queue(msg), d @ Data(None, _, queue)) => {
      stay using d.copy(queue = queue.enqueue(Message(msg,context.sender())))
    }
    case Event(Queue(msg), d @ Data(Some(_),_,Seq())) => {
      goto(Active) using deliverMessages(d.copy(queue = Q(Message(msg,context.sender()))))
    }

  }

  when (Active) {

    case Event(SetRate(rate),d) => {
      this.rate = rate
      stopTimer()
      startTimer(rate)
      stay() using d.copy(callsLeftInThisPeriod = rate.numberOfCalls)
    }

    case Event(SetTarget(None), d) => {
      goto(Idle) using d.copy(target = None)
    }

    case Event(SetTarget(t @Some(_)),d) => {
      stay using d.copy(target = t)
    }

    case Event(Queue(msg), d @Data(_,0,queue)) => {
      stay using d.copy(queue = queue.enqueue(Message(msg,context.sender())))
    }

    case Event(Queue(msg), d @Data(_,_,queue)) => {
      stay using deliverMessages(d.copy(queue = queue.enqueue(Message(msg,context.sender()))))
    }

    case Event(Tick, d @Data(_,_,Seq())) => {
      goto(Idle)
    }

    // Period ends and we get more occasions to send messages
    case Event(Tick,d @Data(_,_,_)) => {
      stay using deliverMessages(d.copy(callsLeftInThisPeriod = rate.numberOfCalls))
    }

  }

  onTransition {
    case Idle -> Active => startTimer(rate)
    case Active -> Idle => stopTimer()
  }

  initialize()

  private def startTimer(rate: Rate) = setTimer("morePermits",Tick,rate.duration,true)
  private def stopTimer() = cancelTimer("morePermits")


  private def deliverMessages(data: Data): Data = {
    val queue = data.queue
    val nrOfMessagesToSend = scala.math.min(queue.length,data.callsLeftInThisPeriod)
    queue.take(nrOfMessagesToSend).foreach((x: Message) => {
      try {
        data.target.get ! (x.message,x.sender)
      } catch {
        case NonFatal(ex) => throw new FailedToSendException("tell() failed.",ex)
      }
    })
    data.copy(queue = queue.drop(nrOfMessagesToSend),callsLeftInThisPeriod = data.callsLeftInThisPeriod - nrOfMessagesToSend)
  }

}

*/
