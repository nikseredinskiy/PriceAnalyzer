package com.nikser.analyzer.dao

import java.util.Date

import com.nikser.analyzer.config.{Item, ItemPrice, Provider}
import com.nikser.analyzer.parser.JsonUtils
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala._
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates.set

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

class ItemPriceDao(mongoClient: MongoClient = Provider.mongoClient) {

  private val itemPriceCodecRegistry = fromRegistries(fromProviders(classOf[ItemPrice]), DEFAULT_CODEC_REGISTRY)
  private val prices: MongoCollection[ItemPrice] = mongoClient.getDatabase("price_chart")
    .withCodecRegistry(itemPriceCodecRegistry)
    .getCollection("item_prices")

  def insertItemPrice(itemPrice: ItemPrice): Future[Completed] = {
    val today = Provider.dateFormat.format(new Date())
    prices.find(and(
      equal("itemId", itemPrice.itemId),
      equal("date", today),
      equal("shopId", itemPrice.shopId)
    )).toFuture().flatMap(seq => {
      if (seq.isEmpty)
        prices.insertOne(itemPrice).toFuture()
      else
        prices.updateOne(and(equal("itemId", itemPrice.itemId), equal("date", today), equal("shopId", itemPrice.shopId)), set("price", itemPrice.price))
          .toFuture()
          .map(_ => Completed())
    })
  }

  def getPriceForItemAndShop(item: Item, shopId: String): String = {
    val source = Source.fromURL(generateShopsUrl(item.key))
    val jsonString = source.getLines().mkString
    val json = JsonUtils.toMapStringAny(JsonUtils.parseJson(jsonString))
    val primaries = json("positions").asInstanceOf[Map[String, Any]]("primary").asInstanceOf[Seq[Map[String, Any]]]
    val priceO = primaries.filter(shop => shop("shop_id").toString == shopId).map(shop => {
      shop("position_price").asInstanceOf[Map[String, Any]]("amount").toString
    }).headOption
    priceO.map(price => price.substring(0, price.indexOf('.'))).getOrElse("0")
  }

  def getItemPricesByUuidAndShopId(itemUuid: String, shopId: String): Future[Seq[ItemPrice]] = {
    prices.find(and(equal("itemId", itemUuid), equal("shopId", shopId))).toFuture()
  }

  private def generateShopsUrl(key: String): String = {
    s"https://catalog.onliner.by/sdapi/shop.api/products/$key/positions?town_id=17030"
  }

}
