package com.knoldus

import java.io.File

import akka.actor.{OneForOneStrategy, SupervisorStrategy}
import akka.actor.SupervisorStrategy.{Escalate, Resume, Stop}
import akka.pattern.AskTimeoutException

import scala.concurrent.duration._

trait Utils {
  def getListOfFiles(inputDirectory: String): List[File] = {
    val d = new File(inputDirectory)
    if (d.exists && d.isDirectory) {
      d.listFiles.toList
    } else if (d.exists && d.isFile) {
      List.empty[File] :+ d
    }else {
      List[File]()
    }
  }
  def writeAnalysisToFile(logAnalysisResult : File,fileName : String,errorCount : Int,warningCount : Int , infoCount : Int) : String = {
    import java.io._
    val bw = new BufferedWriter(new FileWriter(logAnalysisResult,true))
    bw.write(s"$fileName $errorCount $warningCount $infoCount\n")
    bw.close()
    fileName
  }

  def mySupervisorStrategy: SupervisorStrategy = {
    val maxNrOfRetries = 5
    val withinTimeRange = 10.second
    OneForOneStrategy(maxNrOfRetries, withinTimeRange) {
      case _ : AskTimeoutException => Resume
      case _ : Exception => Escalate
    }
  }
}
