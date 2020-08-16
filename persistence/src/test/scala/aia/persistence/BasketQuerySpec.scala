package aia.persistence

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.Await

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

import akka.persistence.query.{EventEnvelope, PersistenceQuery}
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import org.scalatest.wordspec.AnyWordSpecLike

class BasketQuerySpec
  extends PersistenceSpec(ActorSystem("test")) with AnyWordSpecLike{

  val shopperId = 3L
  val macbookPro = Item("Apple Macbook Pro", 1, BigDecimal(2499.99))
  val macPro = Item("Apple Mac Pro", 1, BigDecimal(10499.99))
  val displays = Item("4K Display", 3, BigDecimal(2499.99))
  val appleMouse = Item("Apple Mouse", 1, BigDecimal(99.99))
  val appleKeyboard = Item("Apple Keyboard", 1, BigDecimal(79.99))
  val dWave = Item("D-Wave One", 1, BigDecimal(14999999.99))

  "Querying the journal for a basket" must {

    "do the following" in {

      import system.dispatcher

      val basket = system.actorOf(Basket.props, Basket.name(shopperId))
      basket ! Basket.Add(macbookPro, shopperId)
      basket ! Basket.Add(displays, shopperId)
      basket ! Basket.GetItems(shopperId)
      expectMsg(Items(macbookPro, displays))
      killActors(basket)


      implicit val mat = ActorMaterializer()(system)
      val queries =
        PersistenceQuery(system).readJournalFor[LeveldbReadJournal](
          LeveldbReadJournal.Identifier
        )

      val src: Source[EventEnvelope, NotUsed] = queries.currentEventsByPersistenceId(Basket.name(shopperId), 0, Long.MaxValue)

      val events: Source[Basket.Event, NotUsed] = src.map(_.event.asInstanceOf[Basket.Event])

      val res: Future[Seq[Basket.Event]] = events.runWith(Sink.seq)

      val result = Await.result(res,10.seconds)

      result mustBe Vector(Basket.Added(macbookPro), Basket.Added(displays))

    }
  }

}
