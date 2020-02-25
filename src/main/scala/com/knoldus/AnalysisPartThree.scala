package com.knoldus

import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{Actor, ActorSystem, Props}
import akka.util.Timeout
import akka.pattern.ask
import akka.routing.RoundRobinPool
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source

class AnalysisPartThree extends Actor {

  override def receive: Receive = {
    case file: File => sender ! analyseFile(file)
  }

  def analyseFile(file: File): FileAnalysisResult = {
    val f = Source.fromFile(file).getLines.toList
    val res = f.foldLeft((0, 0, 0)) { (acc, line) => {
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
    FileAnalysisResult(file.getName, res._1, res._2, res._3)
  }
}


object AnalysisPartThreeOb extends App with Utils {

  val system = ActorSystem("AnalysisSystem")

  implicit val timeout = Timeout(1.second)

  val list = getListOfFiles("src/main/resources/log-files")

  val actor = system.actorOf(RoundRobinPool(3).props(Props[AnalysisPartThree]).withDispatcher("fixed-thread-pool"), "myactor")


  val res = list.map(file => {

    (actor ? file).mapTo[FileAnalysisResult].recover {
      case exception: Exception => FileAnalysisResult(file.getName, -1, -1, -1)
    }
  })

  val results = Future.sequence(res)

  Thread.sleep(3000)

  results.map(res => res.map(r => println(r)))

}
