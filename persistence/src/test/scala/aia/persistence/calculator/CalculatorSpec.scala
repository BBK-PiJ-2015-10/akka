package aia.persistence.calculator

import aia.persistence.PersistenceSpec
import aia.persistence.calculator.Calculator.GetResult
import akka.actor.ActorSystem

class CalculatorSpec extends PersistenceSpec(ActorSystem("test")) {

  "The calculator" should {

    "recover last result after crash" in {

      val calc = system.actorOf(Calculator.props,Calculator.name)
      calc ! Calculator.Add(1d)
      calc ! GetResult
      expectMsg(1d)

      calc ! Calculator.Subtract(0.5d)
      calc ! GetResult
      expectMsg(0.5d)

      killActors(calc)

      val calcResurrected = system.actorOf(Calculator.props,Calculator.name)
      calcResurrected ! GetResult
      expectMsg(0.5d)

      calcResurrected ! Calculator.Add(1d)
      calcResurrected ! GetResult
      expectMsg(1.5d)

    }

  }
  


}
