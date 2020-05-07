package com.nikser.analyzer.dao

import com.nikser.analyzer.config.{Provider, UserItem}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala._
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.Filters._

import scala.concurrent.Future

class UserItemDao(mongoClient: MongoClient = Provider.mongoClient) {
  private val userItemCodecRegistry = fromRegistries(fromProviders(classOf[UserItem]), DEFAULT_CODEC_REGISTRY )

  private val userItems: MongoCollection[UserItem] = mongoClient.getDatabase("price_chart")
    .withCodecRegistry(userItemCodecRegistry)
    .getCollection("user_items")

  def insertUserItem(userItem: UserItem): Future[Completed] = {
    userItems.insertOne(userItem).toFuture()
  }

  def getUserItems(userId: Long): Future[Seq[UserItem]] = {
    userItems.find(equal("userId", userId.toString)).toFuture()
  }

}
