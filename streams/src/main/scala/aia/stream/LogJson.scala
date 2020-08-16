package aia.stream

import java.nio.file.{Files,Path}
import java.io.File
import java.time.ZonedDateTime

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.{Success,Failure}

import akka.Done
import akka.actor.{ActorSystem}
import akka.util.ByteString


import akka.stream.scaladsl.JsonFraming
import akka.stream.scaladsl.{BidiFlow,Flow,Framing}

import spray.json._


object LogJson extends EventMarshalling
  with NotificationMarshalling
  with MetricMarshalling {


  def textInFlow(maxLine : Int) = {
    Framing.delimiter(ByteString("\n"),maxLine)
      .map(_.decodeString("UTF-8"))
      .map(LogStreamProcessor.parseLineEx)
      .collect { case Some(e) => e}
  }

  def jsonInFlow(maxJsonObject: Int) = {
    JsonFraming.objectScanner(maxJsonObject)
      .map(_.decodeString("UTF-8").parseJson.convertTo[Event])
  }

  def jsonFramed(maxJsonObject: Int) = JsonFraming.objectScanner(maxJsonObject)

  val jsonOutFlow = Flow[Event].map{ event => ByteString(event.toJson.compactPrint)}

  val notifyOutFlow = Flow[Summary].map{ ws =>ByteString(ws.toJson.compactPrint)}

  val metricOutFlow = Flow[Metric].map{ m => ByteString(m.toJson.compactPrint)}

  val textOutFlow = Flow[Event].map{ event => ByteString(LogStreamProcessor.logLine(event))}

  def logToJson(maxJsonObject : Int) = { BidiFlow.fromFlows(textInFlow(maxJsonObject),jsonOutFlow)}

  def jsonToLog(maxJsonObject: Int) = { BidiFlow.fromFlows(jsonInFlow(maxJsonObject),textOutFlow)}

  def logToJsonFlow(maxLine: Int) = {
    logToJson(maxLine).join(Flow[Event])
  }

  def jsonToLogFlow(maxJsonObject: Int) = {
    jsonToLog(maxJsonObject).join(Flow[Event])
  }





}
