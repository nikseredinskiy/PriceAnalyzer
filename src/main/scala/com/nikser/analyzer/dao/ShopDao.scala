package com.nikser.analyzer.dao

import com.nikser.analyzer.config.{Item, Shop}
import com.nikser.analyzer.parser.JsonUtils

import scala.io.Source

class ShopDao() {

  def getShopsForItem(item: Item): Seq[Shop] = {
    val source = Source.fromURL(generateShopsUrl(item.key))
    val jsonString = source.getLines().mkString
    val json = JsonUtils.toMapStringAny(JsonUtils.parseJson(jsonString))
    val primaries = json("positions").asInstanceOf[Map[String, Any]]("primary").asInstanceOf[Seq[Map[String, Any]]]
    primaries.map(shop => {
      val shopId = shop("shop_id").toString
      val name = json("shops").asInstanceOf[Map[String, Any]](shopId).asInstanceOf[Map[String, Any]]("title").toString
      val price = shop("position_price").asInstanceOf[Map[String, Any]]("amount").toString
      Shop(shopId, name, price)
    })
  }

  def getShopName(shopId: String): String = {
    val source = Source.fromURL(generateShopId(shopId))
    val jsonString = source.getLines().mkString
    val json = JsonUtils.toMapStringAny(JsonUtils.parseJson(jsonString))
    json("title").toString
  }

  private def generateShopId(shopId: String): String = {
    s"https://catalog.onliner.by/sdapi/shop.api/shops/$shopId"
  }

  private def generateShopsUrl(key: String): String = {
    s"https://catalog.onliner.by/sdapi/shop.api/products/$key/positions?town_id=17030"
  }

}
