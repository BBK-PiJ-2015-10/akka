package aia.state



import akka.testkit.{TestKit, TestProbe}
import akka.actor.ActorSystem
import akka.agent.Agent
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers

import scala.concurrent.stm._
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import concurrent.{Await, Future}
import akka.util.Timeout

class AgentTest extends TestKit(ActorSystem("AgentTest"))
  with AnyWordSpecLike with BeforeAndAfterAll with Matchers {

  implicit val ec = system.dispatcher
  implicit val timeout = Timeout(FiniteDuration(3, TimeUnit.SECONDS))

  override protected def afterAll(): Unit = system.terminate()


  "Agent" must {

    /*

    "test1" in {
      val agent = Agent(new StateBookStatistics(0,Map()))

      Future {
        Thread.sleep(2000)
        agent send (new StateBookStatistics(22,Map()))
      }

      println("1: " +agent())
      atomic {
        txn =>
         val value = agent.get
         println("2: "+agent())
         Thread.sleep(5000)
         agent send (new StateBookStatistics(value.sequence+1,Map()))
      }
      println("3: " +agent())
      println("4: " + Await.result(agent.future(),FiniteDuration(1,TimeUnit.SECONDS)))

    }


    "test2" in {
     val agent = Agent(new StateBookStatistics(0,Map()))

     Future {
       Thread.sleep(1000)
       agent send (new StateBookStatistics(22,Map()))
     }

     println("1: " +agent())
     atomic {
       txn =>
         println("2: "+agent())
         Thread.sleep(5000)
         agent.send((oldState) => oldState.copy(sequence = oldState.sequence + 1))
     }
     println("3: "+agent())
     println("4: " + Await.result(agent.future(),FiniteDuration(1,TimeUnit.SECONDS)))


    }



    "test3" in {
      val agent = Agent(new StateBookStatistics(0,Map()))
      val func : (StateBookStatistics => StateBookStatistics) = (oldState: StateBookStatistics) => {oldState.copy(sequence = oldState.sequence +1)}

      agent.send(func)
      println("3: " +agent())
      println("4: " +Await.result(agent.future(),FiniteDuration(1,TimeUnit.SECONDS)))

    }


    "test4" in {

      val probe = TestProbe()
      val agent = Agent(new StateBookStatistics(0,Map()))
      val func : (StateBookStatistics => StateBookStatistics) = (oldState: StateBookStatistics) => {
        if (oldState.sequence == 0) {
          probe.ref ! "test"
        }
        oldState.copy(sequence = oldState.sequence +1)
      }

      agent.send(func)
      println("3: "+agent())
      probe.expectMsg("test")
      println("4 : "+agent())

    }

    "test5" in {

      val agent1 = Agent(3)

      val agent4 = agent1 map (_ + 1)

      println("1: "+agent4())

      agent1 send (_ + 2)
      println("2: "+Await.result(agent1.future(),FiniteDuration(1,TimeUnit.SECONDS)))

      println("3: "+Await.result(agent4.future(),FiniteDuration(1,TimeUnit.SECONDS)))

    }

    */

  "test alter" in {
    val bookName = "Akka in Action"
    val mgr = new BookStatisticsMgr(system)
    mgr.addBooksSold(bookName, 1)
    val state = mgr.addBookSoldAndReturnNewState(bookName, 1)
    val book = new BookStatistics(bookName, 2)
    state must be(new StateBookStatistics(2, Map(bookName -> book)))
  }


  }

}
