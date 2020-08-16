package aia.state

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.agent.Agent

import concurrent.duration._
import concurrent.Await

class BookStatisticsMgr(system: ActorSystem) {

  implicit val ex = system.dispatcher
  val stateAgent = Agent(new StateBookStatistics(0,Map()))

  def getBookStatistics() : StateBookStatistics = stateAgent.get()

  def addBooksSold(book: String, nrSold: Int) : Unit = {
    stateAgent send (oldState => {
      val bookStat = oldState.books.get(book) match {
        case Some(bookState) =>
          bookState.copy(nrSold = bookState.nrSold + nrSold)
        case None => new BookStatistics(book,nrSold)
      }
      oldState.copy(oldState.sequence + 1,
        oldState.books + (book -> bookStat))
    })
  }

  def addBookSoldAndReturnNewState(book: String, nrSold: Int) : StateBookStatistics ={
    val future = stateAgent alter (oldState => {
      val bookStat = oldState.books.get(book) match {
        case Some(bookState) =>
          bookState.copy(nrSold = bookState.nrSold + nrSold)
        case None => new BookStatistics(book,nrSold)
      }
      oldState.copy(oldState.sequence +1,
        oldState.books + (book -> bookStat))
    })
    Await.result(future,FiniteDuration(1,TimeUnit.SECONDS))
  }

}
