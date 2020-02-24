package com.knoldus

import java.io.File

import scala.io.Source

class AnalysisPartOne {

  def getListOfFiles(inputDirectory : String): List[File] = {
    val d = new File(inputDirectory)
    if (d.exists && d.isDirectory) {
      d.listFiles.toList
    } else {
      List[File]()
    }
  }

  def readDataFromFiles(listOfFiles : List[File]) : List[(String,List[String])] = for{
    file<-listOfFiles
    if(file.isFile)
  } yield (file.getName,Source.fromFile(file).getLines.toList)

  def getAnalysisResult(filesList : List[(String,List[String])]) : List[(String,(Int,Int,Int))] = {
    @scala.annotation.tailrec
    def doAnalysis(filesList : List[(String,List[String])], res : List[(String,(Int,Int,Int))]): List[(String,(Int,Int,Int))] = {
      filesList match {
        case file :: rest => doAnalysis(rest,res :+ (file._1,(file._2.count(_.contains("ERROR")),file._2.count(_.contains("WARN")),file._2.count(_.contains("INFO")))))
        case file :: Nil  => res :+ (file._1,(file._2.count(_.contains("ERROR")),file._2.count(_.contains("WARN")),file._2.count(_.contains("INFO"))))
        case Nil          => res
      }
    }
    doAnalysis(filesList,List.empty[(String,(Int,Int,Int))])
  }

  def getAverageErrorPerFile(analysisResult : List[(String,(Int,Int,Int))]) : Int ={
    analysisResult.foldLeft(0){(errorSum : Int,log : (String,(Int,Int,Int))) => errorSum+log._2._1}/analysisResult.length
  }
}
object AnalysisPartOneOb extends App{
  val ob = new AnalysisPartOne
  val list=ob.getListOfFiles("src/main/resources/log-files")
  val filesList = ob.readDataFromFiles(list)
  println(filesList(0)._1 + "\t"+filesList(0)._2.length )
  val analysisResult = ob.getAnalysisResult(filesList)
  val averageErrorPerFile = ob.getAverageErrorPerFile(analysisResult)
  println(averageErrorPerFile)

}
