package com.organizaion.statistics.Models

sealed trait SensorData {
  def sensor_id: String
  def humidity : Any
}

case class SensorDataValid (sensor_id: String ,humidity: Int) extends SensorData

case class SensorDataCorrupted (sensor_id: String ,humidity: String) extends SensorData

case class ReportSensorData(sensor_id: String, min: String, avg: String, max :String)

