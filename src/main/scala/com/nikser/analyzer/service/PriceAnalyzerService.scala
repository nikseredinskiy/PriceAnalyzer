package com.nikser.analyzer.service

import java.util.Date
import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Scheduler}
import com.bot4s.telegram.models.{CallbackQuery, InputFile}
import com.nikser.analyzer.chart.PriceChart
import com.nikser.analyzer.dao.{ItemDao, ItemPriceDao, ShopDao, UserItemDao}
import com.nikser.analyzer.parser.OnlinerParser
import com.nikser.analyzer.config.{Item, ItemPrice, Provider, Shop, UserItem}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContextExecutor, Future}

class PriceAnalyzerService(
                            drawService: DrawService = Provider.drawService,
                            itemDao: ItemDao = Provider.itemDao,
                            itemPriceDao: ItemPriceDao = Provider.itemPriceDao,
                            userItemDao: UserItemDao = Provider.userItemDao,
                            shopDao: ShopDao = Provider.shopDao,
                            onlinerParser: OnlinerParser = Provider.onlinerParser,
                            priceChart: PriceChart = Provider.priceChart
                          ) {

  private val actorSystem: ActorSystem = ActorSystem()
  private val scheduler: Scheduler = actorSystem.scheduler
  implicit val executor: ExecutionContextExecutor = actorSystem.dispatcher

  val task: Runnable = () => {
    getAllItems.map { items =>
      items.foreach(item => {
        createAndInsertItemPrice(item._1, item._2).map { _ => {
          val shopName = shopDao.getShopName(item._2)
          println(s"Added price for ${item._1.name} in $shopName")
        }}
      })
    }
  }

  scheduler.schedule(initialDelay = Duration(1, TimeUnit.DAYS), interval = Duration(1, TimeUnit.DAYS), runnable = task)

  def addItem(url: String): Future[(String, Seq[Shop])] = {
    val key = onlinerParser.parseKey(url)
    itemDao.getItemByKey(key).flatMap {
      case Some(item) =>
        Future.successful((item.key, shopDao.getShopsForItem(item)))
      case None =>
        for {
          item <- createAndInsertItem(url)
          shops = shopDao.getShopsForItem(item)
        } yield (item.key, shops)
    }
  }

  def addPrices(userId: Long): Future[String] = {
    getAllItems(userId).flatMap { items =>
      Future.sequence(items.map(item => {
        createAndInsertItemPrice(item._1, item._2).map { _ =>
          s"Added price for ${item._1.name}"
        }
      })).map(_.mkString("\n"))
    }
  }

  def addPrices(): Future[String] = {
    getAllItems.flatMap { items =>
      Future.sequence(items.map(item => {
        createAndInsertItemPrice(item._1, item._2).map { _ =>
          s"Added price for ${item._1.name}"
        }
      })).map(_.mkString("\n"))
    }
  }

  def handleShopCallback(callback: CallbackQuery): Future[Option[String]] = {
    val onlinerShopIdOrDefault = callback.data.getOrElse("0")
    val userId = callback.from.id

    callback.message.flatMap({ message =>
      message.text.map({ text =>
        val itemKey = parseTextToGetItemKey(text)
        itemDao.getItemByKey(itemKey).flatMap({
          case Some(item) =>
            userItemDao.getUserItems(userId).map(_.exists(i => i.shopId == onlinerShopIdOrDefault && i.itemId == item.itemId)).flatMap(isExists => {
              if (isExists) {
                Future.successful("You've already had this device")
              } else {
                for {
                  _ <- createAndInsertItemPrice(item, onlinerShopIdOrDefault)
                  _ <- linkUserToDeviceAndShop(userId, item.itemId, onlinerShopIdOrDefault)
                } yield "Device was added"
              }
            })
          case None =>
            Future.successful(s"No item found for key $itemKey")
        })
      })
    }) match {
      case Some(f) => f.map(Some(_))
      case None => Future.successful(None)
    }
  }

  def draw(userId: Long, deviceKey: String): Future[Seq[InputFile]] = {
    getAllItems(userId).filter(_.exists(_._1.key == deviceKey)).flatMap(getInputFiles)
  }

  def drawAll(userId: Long): Future[Seq[InputFile]] = {
    getAllItems(userId).flatMap(getInputFiles)
  }

  def drawAll(): Future[Seq[InputFile]] = {
    getAllItems.flatMap(getInputFiles)
  }

  private def getInputFiles(items: Seq[(Item, String)]): Future[Seq[InputFile]] = {
    Future.sequence(items.map(item => {
      println("Drawing chart for: " + item._1.name + " in " + item._2)
      drawService.drawChart(item._1, item._2).map { info =>
        info._2.map(image => {
          InputFile(s"${info._1}-chart.png", image)
        })
      }
    })).map(_.flatten)
  }

  def parseTextToGetItemKey(text: String): String = {
    text.substring(15, text.indexOf(')'))
  }

  def createAndInsertItem(url: String): Future[Item] = {
    val item = createItem(url)
    itemDao.insertItem(item).map(_ => item)
  }

  def createAndInsertItemPrice(item: Item, shopId: String): Future[ItemPrice] = {
    val itemPrice = createItemPrice(item, shopId)
    itemPriceDao.insertItemPrice(itemPrice).map(_ => itemPrice)
  }

  def linkUserToDeviceAndShop(userId: Long, itemId: String, shopId: String): Future[UserItem] = {
    val userItem = UserItem(userId.toString, itemId, shopId)
    userItemDao.insertUserItem(userItem).map(_ => userItem)
  }

  def getAllItems(userId: Long): Future[Seq[(Item, String)]] = {
    itemDao.getAllItems(userId)
  }

  def getAllItems: Future[Seq[(Item, String)]] = {
    itemDao.getAllItems
  }

  def getAllItemsKeys: Future[Seq[String]] = {
    itemDao.getAllItemsKeys
  }

  private def createItem(url: String): Item = {
    Item(
      generateDeviceUuid(),
      getDeviceName(url),
      onlinerParser.parseKey(url),
      url
    )
  }

  private def createItemPrice(item: Item, shopId: String): ItemPrice = {
    ItemPrice(
      item.itemId,
      getDevicePrice(item, shopId),
      generateCurrentDate(),
      shopId
    )
  }

  private def generateDeviceUuid(): String = {
    java.util.UUID.randomUUID.toString
  }

  private def getDeviceName(url: String): String = {
    itemDao.getItemName(onlinerParser.parseKey(url))
  }

  private def getDevicePrice(item: Item, shopId: String): String = {
    itemPriceDao.getPriceForItemAndShop(item, shopId)
  }

  private def generateCurrentDate(): String = {
    Provider.dateFormat.format(new Date())
  }


}
