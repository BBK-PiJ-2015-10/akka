package aia.persistence.calculator

import akka.actor.{ActorLogging, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted}

object Calculator {

  def props = Props(new Calculator())

  def name = "my-calculator"

  sealed trait Command
  case object Clear extends Command
  case class Add(value: Double) extends Command
  case class Subtract(value: Double) extends Command
  case class Divide(value: Double) extends Command
  case class Multiply(value: Double) extends Command
  case object PrintResult extends Command
  case object GetResult extends Command

  sealed trait Event
  case object Reset extends Event
  case class Added(value: Double) extends Event
  case class Subtracted(value: Double) extends Event
  case class Divided(value: Double) extends Event
  case class Multiplied(value: Double) extends Event

  case class CalculationResult(result: Double = 0.doubleValue()){
    def reset = copy (result = 0)
    def add(value: Double) = copy(result = this.result + value)
    def subtract(value: Double) = copy (result = this.result - value)
    def multiply(value: Double) = copy (result = this.result * value)
    def divide(value: Double) = copy (result = this.result / value)

  }

}

class Calculator extends PersistentActor with ActorLogging {
  import Calculator._

  override def persistenceId: String = Calculator.name

  var state = CalculationResult()

  override def receiveCommand: Receive = {
    case Add(value) => persist(Added(value))(updateState)
    case Subtract(value) => persist(Subtracted(value))(updateState)
    case Divide(value) => if (value!=0) persist(Divided(value))(updateState)
    case Multiply(value) => persist(Multiplied(value))(updateState)
    case Clear => persist(Reset)(updateState)
    case GetResult => sender() ! state.result
    case PrintResult => println(s"the result is: ${state.result}")
  }

  override def receiveRecover: Receive = {
    case event : Event => updateState(event)
    case RecoveryCompleted => log.info("Calculator recovery completed")
  }

  val updateState: Event => Unit = {
    case Added(value) => state = state.add(value)
    case Subtracted(value) => state = state.subtract(value)
    case Divided(value) => state = state.divide(value)
    case Multiplied(value) => state = state.multiply(value)
    case Reset => state = state.reset
  }

}
