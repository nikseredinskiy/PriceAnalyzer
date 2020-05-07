package com.nikser.analyzer.config

import java.text.SimpleDateFormat

import com.nikser.analyzer.chart.PriceChart
import com.nikser.analyzer.dao.{ItemDao, ItemPriceDao, ShopDao, UserItemDao}
import com.nikser.analyzer.parser.OnlinerParser
import com.nikser.analyzer.service.{DrawService, PriceAnalyzerService}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import org.mongodb.scala.MongoClient

case class Item(itemId: String, name: String, key: String, url: String)
case class ItemPrice(itemId: String, price: String, date: String, shopId: String)
case class UserItem(userId: String, itemId: String, shopId: String)
case class Shop(onlinerId: String, name: String, price: String)

object Provider {

  lazy val invalidMessage = "Invalid argumentヽ(ಠ_ಠ)ノ"

  lazy val mongoUrl = scala.util.Properties.envOrElse("MONGO_URL", "")
  lazy val mongoClient: MongoClient = MongoClient(mongoUrl)
  lazy val dateFormat = new SimpleDateFormat("dd-MM-yyyy")

  lazy val browser = JsoupBrowser()

  lazy val itemDao = new ItemDao()
  lazy val itemPriceDao = new ItemPriceDao()
  lazy val userItemDao = new UserItemDao()
  lazy val shopDao = new ShopDao()

  lazy val onlinerParser = new OnlinerParser()
  lazy val priceChart = new PriceChart()

  lazy val priceAnalyzerService = new PriceAnalyzerService()
  lazy val drawService = new DrawService()

}
