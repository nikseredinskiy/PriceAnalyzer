package com.nikser.analyzer.service

import com.nikser.analyzer.chart.PriceChart
import com.nikser.analyzer.config.{Item, Provider}
import com.nikser.analyzer.dao.ItemPriceDao
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class DrawService(
                   itemPriceDao: ItemPriceDao = Provider.itemPriceDao,
                   priceChart: PriceChart = Provider.priceChart
                 ) {

  def drawChart(item: Item, shopId: String): Future[(String, Seq[Array[Byte]])] = {
    val itemPricesF = itemPriceDao.getItemPricesByUuidAndShopId(item.itemId, shopId)
    itemPricesF.map { itemPrices => {
      println(s"ItemPrices count : ${itemPrices.size}")
      val datePrices = itemPrices.groupBy(_.shopId).map(k => {
        println(s"Dates for ${k._1} = ${k._2.map(_.date).mkString(" | ")}")
        k._1 -> k._2.map(i => (i.date, i.price.toInt))
      })
      (item.key, priceChart.drawChart(item, datePrices))
    }}
  }

}
