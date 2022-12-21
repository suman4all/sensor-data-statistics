package com.organizaion.statistics.Akka
import com.organizaion.statistics.Models._

import java.nio.file.{FileSystems, Paths}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

object Main extends App{

  val logger = Logger(LoggerFactory.getLogger(""))


  // Initializing Akka ActorSystem
  implicit val actorSystem = ActorSystem()
  implicit val mat = ActorMaterializer()
  import actorSystem.dispatcher


  //Fetching Environment Variables
  private val inputDirectory = Paths.get(Config().getString("env.input-directory")).toFile
  private val linesToSkip = Config().getInt("env.linesToSkip")
  private val awaitTimeInSeconds = Config().getInt("env.awaitTimeInSeconds")

/* Required for Balancer
  private val concurrentFilesRead = Config().getInt("env.concurrentFilesRead")
  private val nonIOParallelism = Config().getInt("env.nonIOParallelism")*/




  val files = inputDirectory.listFiles.toList

  var validMeasureCount = 0
  var failedMeasureCount = 0
  var sd_valid_list = List.empty[SensorDataValid]
  var sd_corrupted_list = List.empty[SensorDataCorrupted]

  for (file <- files) {

    // Code For reading a CSV Files using Akka Stream
    val logFile = Paths.get(s"${file.getPath}")

    val source = FileIO.fromPath(logFile)

    val sink5 = Sink.fold[List[SensorDataValid], SensorDataValid](List.empty[SensorDataValid])((l, e) => e :: l)
    val sink6 = Sink.fold[List[SensorDataCorrupted], SensorDataCorrupted](List.empty[SensorDataCorrupted])((l, e) => e :: l)
    val sink3 = Sink.fold[Int, SensorDataValid](0)((acc, _) => acc + 1)
    val sink4 = Sink.fold[Int, SensorDataCorrupted](0)((acc, _) => acc + 1)


    val source_not_filtered = source
      .via(CsvParsing.lineScanner())
      .drop(linesToSkip)
      .map(_.map(_.utf8String))
      .map { case List(key, value) => key -> value }


    // For Valid Measurements
    val (validRowData,validRowCount) = source_not_filtered
      .filter(x => x._2 != "NaN")
      .map { case (key, value) =>  SensorDataValid(key,value.toInt)}
      .alsoToMat(sink5)(Keep.right)
      .toMat(sink3)(Keep.both)
      .run()

    // For Invalid Measurements
    val (corruptedRowData,corruptedRowCount) = source_not_filtered
      .filter(x => x._2 == "NaN")
      .map { case (key, value) =>  SensorDataCorrupted(key,value)}
      .alsoToMat(sink6)(Keep.right)
      .toMat(sink4)(Keep.both)
      .run()



    val fileValidMeasureCount =  Await.result(validRowCount, awaitTimeInSeconds seconds)
    validMeasureCount = validMeasureCount + fileValidMeasureCount

    val fileFailedMeasureCount =  Await.result(corruptedRowCount, awaitTimeInSeconds seconds)
    failedMeasureCount = failedMeasureCount + fileFailedMeasureCount

    val file_sd_valid_list =  Await.result(validRowData, awaitTimeInSeconds seconds)
    sd_valid_list = List.concat(sd_valid_list,file_sd_valid_list)

    val file_sd_corrupted_list =  Await.result(corruptedRowData, awaitTimeInSeconds seconds)
    sd_corrupted_list = List.concat(sd_corrupted_list,file_sd_corrupted_list )

  }

  // Aggregating the results
  def applyAggregateMinAvgMax( sensorList :List[SensorDataValid], sensorCorrupted_List:List[SensorDataCorrupted] ): List[ReportSensorData] ={

    val validReportData = sensorList.groupBy(d=>(d.sensor_id)).map { case (k,v) =>
      ReportSensorData(v.head.sensor_id,v.map(_.humidity).min.toString,(v.map(_.humidity).sum/v.map(_.humidity).size).toString,v.map(_.humidity).max.toString)
    }.toList
    val corruptedReportData =
    sensorCorrupted_List.groupBy(d=>(d.sensor_id)).map { case (k,v) =>
      ReportSensorData(v.head.sensor_id,"NaN","NaN","NaN")
    }.toList
    val finalReportData = List.concat(validReportData,corruptedReportData)
    finalReportData.groupBy(d=>(d.sensor_id)).map { case (k,v) =>
      ReportSensorData(v.head.sensor_id,min = v.map(_.min).min , avg = v.map(_.avg).min ,max = v.map(_.max).min )
    }.toList
  }

  val ReportDataRows = applyAggregateMinAvgMax(sd_valid_list,sd_corrupted_list)

  //Printing Data Statistics Report using log
  logger.info("Generating Sensor Data Statistics Report")
  logger.info(s"Num of processed files: ${files.size} ")
  logger.info(s"Num of processed measurements: ${validMeasureCount + failedMeasureCount}")
  logger.info(s"Num of failed measurements: ${failedMeasureCount}")
  logger.info ("sensor-id   min   avg   max")
  for( i <- ReportDataRows){
    logger.info (s"${i.sensor_id}          ${i.min}    ${i.avg}    ${i.max} ")
  }

}
