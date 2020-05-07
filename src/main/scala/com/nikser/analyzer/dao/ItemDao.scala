package com.nikser.analyzer.dao

import com.nikser.analyzer.config.{Item, Provider, UserItem}
import com.nikser.analyzer.parser.JsonUtils
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala._
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.Filters._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

class ItemDao(mongoClient: MongoClient = Provider.mongoClient) {

  private val itemCodecRegistry = fromRegistries(fromProviders(classOf[Item]), DEFAULT_CODEC_REGISTRY )
  private val userItemCodecRegistry = fromRegistries(fromProviders(classOf[UserItem]), DEFAULT_CODEC_REGISTRY )

  private val items: MongoCollection[Item] = mongoClient.getDatabase("price_chart")
    .withCodecRegistry(itemCodecRegistry)
    .getCollection("items")

  private val userItems: MongoCollection[UserItem] = mongoClient.getDatabase("price_chart")
    .withCodecRegistry(userItemCodecRegistry)
    .getCollection("user_items")

  def insertItem(item: Item): Future[Completed] = {
    items.insertOne(item).toFuture()
  }

  def getAllItems(userId: Long): Future[Seq[(Item, String)]] = {
    userItems.find(equal("userId", userId.toString)).toFuture().flatMap(seq => {
      Future.sequence(seq.map(ui =>
        items.find(equal("itemId", ui.itemId)).head().map { item =>
          (item, ui.shopId)
        }
      ))
    })
  }

  def getAllItems: Future[Seq[(Item, String)]] = {
    userItems.find().toFuture().flatMap(seq => {
      Future.sequence(seq.map(ui =>
        items.find(equal("itemId", ui.itemId)).head().map { item =>
          (item, ui.shopId)
        }
      ))
    })
  }

  def getAllItemsKeys: Future[Seq[String]] = {
    items.find().toFuture().map(_.map(_.key))
  }

  def getItemByKey(key: String): Future[Option[Item]] = {
    items.find(equal("key", key)).toFuture().map(_.headOption)
  }

  def getItemName(key: String): String = {
    val source = Source.fromURL(generateItemUrl(key))
    val jsonString = source.getLines().mkString
    val json = JsonUtils.toMapStringAny(JsonUtils.parseJson(jsonString))
    json("extended_name").toString
  }

  private def generateItemUrl(key: String): String = {
    s"https://catalog.onliner.by/sdapi/catalog.api/products/$key"
  }

}
