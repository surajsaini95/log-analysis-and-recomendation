package com.knoldus

import java.io.File

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.util.Timeout
import akka.pattern.ask
import akka.pattern.pipe

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source

class AnalysisPartTwo extends Actor with ActorLogging {

  def getListOfFiles(inputDirectory: String): List[File] = {
    val d = new File(inputDirectory)
    if (d.exists && d.isDirectory) {
      d.listFiles.toList
    } else {
      List[File]()
    }
  }

  def readDataFromFiles(listOfFiles: List[File]): List[(String, List[String])] = for {
    file <- listOfFiles
    if (file.isFile)
  } yield (file.getName, Source.fromFile(file).getLines.toList)

  def getAnalysisResult(filesList: List[(String, List[String])]): List[(String, (Int, Int, Int))] = {
    @scala.annotation.tailrec
    def doAnalysis(filesList: List[(String, List[String])], res: List[(String, (Int, Int, Int))]): List[(String, (Int, Int, Int))] = {
      filesList match {
        case file :: rest => doAnalysis(rest, res :+ (file._1, (file._2.count(_.contains("ERROR")), file._2.count(_.contains("WARN")), file._2.count(_.contains("INFO")))))
        case file :: Nil => res :+ (file._1, (file._2.count(_.contains("ERROR")), file._2.count(_.contains("WARN")), file._2.count(_.contains("INFO"))))
        case Nil => res
      }
    }

    doAnalysis(filesList, List.empty[(String, (Int, Int, Int))])
  }

  def getAverageErrorPerFile(analysisResult: List[(String, (Int, Int, Int))]): Int = {
    analysisResult.foldLeft(0) { (errorSum: Int, log: (String, (Int, Int, Int))) => errorSum + log._2._1 } / analysisResult.length
  }

  override def receive: Receive = {
    case "analyse" => val result = getAnalysisResult(readDataFromFiles(getListOfFiles("src/main/resources/log-files")))
      log.info("Analysis result".toString)
      result.map(res => log.info(res._1 + "\t\t" + res._2._1 + "\t\t" + res._2._2 + "\t\t" + res._2._3.toString))
    case "avgError" => val avgError = getAverageErrorPerFile(getAnalysisResult(readDataFromFiles(getListOfFiles("src/main/resources/log-files"))))
      log.info(s"average error : $avgError".toString)
    case _ => log.info("undefined operation".toString)
  }

}

object LogAnalyserOb extends App {

  val system = ActorSystem("LogAnalysisSystem")
  val props = Props[AnalysisPartTwo]

  val logActor = system.actorOf(props, "AnalysisPartTwo")

  implicit val timeout = Timeout(10.second)

  logActor ! "analyse"
  logActor ! "avgError"


}
