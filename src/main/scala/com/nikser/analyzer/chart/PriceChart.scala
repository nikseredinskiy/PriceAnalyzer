package com.nikser.analyzer.chart

import com.nikser.analyzer.chart.MyChart._
import com.nikser.analyzer.config.{Item, Provider}
import com.nikser.analyzer.dao.ShopDao
import scalax.chart.api._
import org.jfree.data.time.{Day, TimeSeries}

class PriceChart(shopDao: ShopDao = Provider.shopDao) {

  def drawChart(item: Item, data: Map[String, Seq[(String, Int)]]): Seq[Array[Byte]] = {
    println("Got info: " + data)
    data.map(k => {
      val timeSeries = convertToTimeSeries(item.name, k._2)
      val lineChart = MyXYLineChart(timeSeries)
      val shopName = shopDao.getShopName(k._1)
      lineChart.title = s"${item.name} | $shopName"

      println("Drawing chart...")
      lineChart.encodeAsPNG()
    }).toSeq
  }

  private def convertToTimeSeries(key: String, data: Seq[(String, Int)]) = {
    val result = new TimeSeries(key)
    data.foreach(item => {
      val d = item._1.split("-")
      result.add(new Day(d.head.toInt, d.tail.head.toInt, d.tail.tail.head.toInt), item._2)
    })
    result
  }

}
