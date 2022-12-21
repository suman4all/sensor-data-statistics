package com.org.stats.AkkaTest
import org.scalatest.{FlatSpec, Matchers}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import com.organizaion.statistics.Akka.Main.{applyAggregateMinAvgMax, sd_corrupted_list}
import com.organizaion.statistics.Models.{ReportSensorData, SensorDataCorrupted, SensorDataValid}
import com.typesafe.config.ConfigFactory


class SensorDataTest extends FlatSpec with Matchers {

  implicit val system = ActorSystem("SensorDataTest")
  implicit val materializer = ActorMaterializer()



  it should "test applyAggregateMinAvgMax() with all valid measurements " in {
    // given
    val validList = List(
      SensorDataValid("s1", 10),
      SensorDataValid("s2", 30),
      SensorDataValid("s9", 35),
      SensorDataValid("s9", 55),
      SensorDataValid("s2", 80),
      SensorDataValid("s1", 90),
      SensorDataValid("s9", 45)
    )
    val corrupted_list = List.empty[SensorDataCorrupted]

    // when
    val testResult =  applyAggregateMinAvgMax(validList,corrupted_list)

    // then
    val expectedResult = List(
      ReportSensorData("s2","30","55","80"),
      ReportSensorData("s1","10","50","90"),
      ReportSensorData("s9","35","45","55")
    )

    testResult should equal (expectedResult)
    testResult.size should equal(3)
  }

  it should "test applyAggregateMinAvgMax() with all invalid measurements " in {
    // given
    val validList = List.empty[SensorDataValid]
    val corrupted_list = List(
      SensorDataCorrupted("s5", "NaN"),
      SensorDataCorrupted("s1", "NaN"),
      SensorDataCorrupted("s2", "NaN"),
      SensorDataCorrupted("s9", "NaN")
    )
    // when
    val testResult =  applyAggregateMinAvgMax(validList,corrupted_list)

    // then
    val expectedResult = List(
      ReportSensorData("s5","NaN","NaN","NaN"),
      ReportSensorData("s2","NaN","NaN","NaN"),
      ReportSensorData("s1","NaN","NaN","NaN"),
      ReportSensorData("s9","NaN","NaN","NaN")
    )

    testResult should equal (expectedResult)
    testResult.size should equal(4)
  }

  it should "test applyAggregateMinAvgMax() with valid and invalid measurements " in {
    // given
    val validList = List(
      SensorDataValid("s1", 10),
      SensorDataValid("s2", 30),
      SensorDataValid("s9", 35),
      SensorDataValid("s9", 55),
      SensorDataValid("s2", 80),
      SensorDataValid("s1", 90),
      SensorDataValid("s9", 45)
    )
    val corrupted_list = List(
      SensorDataCorrupted("s5", "NaN"),
      SensorDataCorrupted("s1", "NaN"),
      SensorDataCorrupted("s2", "NaN"),
      SensorDataCorrupted("s9", "NaN")
    )
    // when
    val testResult =  applyAggregateMinAvgMax(validList,corrupted_list)

    // then
    val expectedResult = List(
      ReportSensorData("s5","NaN","NaN","NaN"),
      ReportSensorData("s2","30","55","80"),
      ReportSensorData("s1","10","50","90"),
      ReportSensorData("s9","35","45","55")
    )

    testResult should equal (expectedResult)
    testResult.size should equal(4)
  }



}
