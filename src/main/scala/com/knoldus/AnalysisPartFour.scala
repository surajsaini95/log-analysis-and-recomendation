package com.knoldus

import java.io.File

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.util.Timeout
import akka.pattern.{AskTimeoutException, ask}
import akka.routing.RoundRobinPool

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source


case class ScheduleMsg(msg : String)

class AnalysisPartFour extends Actor with Utils with ActorLogging {

  val logAnalysisResult : File = getListOfFiles("src/main/resources/log-analysis-result").head

  override def receive: Receive = {
    case file: File => log.info("passed : " + self.path)
                          sender ! analyseFile(file)
    case "Fail" =>  log.info("failed : " + self.path)
      throw new AskTimeoutException("AskTimeoutException due to overflow of mailbox")
    case scheduleMsg : ScheduleMsg if scheduleMsg.msg.equalsIgnoreCase("displayResult") => log.info("\n\n\n" + displayResult.toString)
    case scheduleMsg : ScheduleMsg if scheduleMsg.msg.equalsIgnoreCase("cleanFile") => cleanFile
  }

  def cleanFile : Boolean = {
    import java.io._
    try{
      val bw = new BufferedWriter(new FileWriter(logAnalysisResult))
      bw.write("")
      bw.close()
      true
    }catch {
      case _ : FileNotFoundException => false
    }
  }

  def analyseFile(file: File): FileAnalysisResult = {
    val fileContent = Source.fromFile(file).getLines.toList
    val res = fileContent.foldLeft((0, 0, 0)) { (acc, line) => {
      if (line.contains("ERROR")) {
        (acc._1 + 1, acc._2, acc._3)
      }
      else if (line.contains("WARN")) {
        (acc._1, acc._2 + 1, acc._3)
      }
      else if (line.contains("INFO")) {
        (acc._1, acc._2, acc._3 + 1)
      }
      else {
        acc
      }
    }
    }

    FileAnalysisResult(writeAnalysisToFile(logAnalysisResult,file.getName, res._1, res._2, res._3), res._1, res._2, res._3)
  }

  def displayResult : String = {
    val fileContent = Source.fromFile(logAnalysisResult).getLines.mkString("\n")
    fileContent
  }
}


object AnalysisPartFourOb extends App with Utils {

  val system = ActorSystem("AnalysisSystem")

  val props =Props[AnalysisPartFour]
  implicit val timeout = Timeout(2.second)


  val list = getListOfFiles("src/main/resources/log-files")

  val actor = system.actorOf(props.withRouter(RoundRobinPool(3,supervisorStrategy = mySupervisorStrategy)).withDispatcher("fixed-thread-pool"), "myactor")
  system.scheduler.scheduleOnce(0.second,actor,ScheduleMsg("cleanFile"))

  val logAnalysisResult : File = getListOfFiles("src/main/resources/log-analysis-result").head

  val res = list.map(file => {

    (actor ? file).mapTo[FileAnalysisResult].recover {
      case exception: AskTimeoutException =>   actor ! "Fail"
        FileAnalysisResult(writeAnalysisToFile(logAnalysisResult,file.getName,-1,-1,-1), -1, -1, -1)
    }
  })

  val results = Future.sequence(res)

  system.scheduler.scheduleWithFixedDelay(2.second,300.second,actor,ScheduleMsg("displayResult"))

}
