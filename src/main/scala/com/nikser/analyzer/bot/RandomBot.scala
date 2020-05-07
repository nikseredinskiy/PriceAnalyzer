package com.nikser.analyzer.bot

import cats.instances.future._
import cats.syntax.functor._
import com.bot4s.telegram.api.RequestHandler
import com.bot4s.telegram.api.declarative.{Callbacks, Commands}
import com.bot4s.telegram.clients.ScalajHttpClient
import com.bot4s.telegram.future.{Polling, TelegramBot}
import com.bot4s.telegram.methods.{SendMessage, SendPhoto}
import com.bot4s.telegram.models.{InlineKeyboardButton, InlineKeyboardMarkup, InputFile}
import com.nikser.analyzer.config.{Item, Provider, Shop}
import com.nikser.analyzer.service.PriceAnalyzerService
import slogging._

import scala.concurrent.Future

class RandomBot(
                 val token: String,
                 priceAnalyzerService: PriceAnalyzerService = Provider.priceAnalyzerService
               ) extends TelegramBot
  with Polling
  with Commands[Future]
  with Callbacks[Future] {

  LoggerConfig.factory = PrintLoggerFactory()
  LoggerConfig.level = LogLevel.TRACE

  override val client: RequestHandler[Future] = new ScalajHttpClient(token)

  val SHOP_TAG = "SHOP_TAG"
  def shopTag: String => String = prefixTag(SHOP_TAG)

  def shopMarkup(shops: Seq[Shop]): InlineKeyboardMarkup = {
      InlineKeyboardMarkup.singleColumn(
        shops.map(shop => {
          InlineKeyboardButton.callbackData(s"${shop.name} | Price: ${shop.price}", shopTag(shop.onlinerId))
        })
      )
  }

  onCallbackWithTag(SHOP_TAG) { implicit cbq =>
    println("LET'S GO WITH CALLBACK")
    priceAnalyzerService.handleShopCallback(cbq).map {
      case Some(message) => request(SendMessage(cbq.message.get.source, message))
      case None => request(SendMessage(cbq.message.get.source, "Oops, something goes wrong"))
    }
  }

  onCommand("devices") { implicit msg =>
    priceAnalyzerService.getAllItems(msg.source).map(items => {
      items.map(_._1).toSet[Item].foreach(item =>
        reply(s"${item.name} | ${item.key}").void
      )
    })
  }

  onCommand("adddevice") { implicit msg =>
    withArgs {
      case a if a.mkString(" ").contains("catalog.onliner") =>
        priceAnalyzerService.addItem(a.mkString("")).map(resp =>
          reply(text = s"Item was added(${resp._1}). Please choose a shop:", replyMarkup = Some(shopMarkup(resp._2))).void
        )
      case _ => reply(s"${Provider.invalidMessage}\nPlease provide catalog.onliner link").void
    }
  }

  onCommand("addprices") { implicit msg =>
    priceAnalyzerService.addPrices(msg.source).map(message =>
      reply(message).void
    )
  }

  onCommand("addprices_admin") { implicit msg =>
    priceAnalyzerService.addPrices().map(message =>
      reply(message).void
    )
  }

  onCommand("drawall") { implicit msg =>
    priceAnalyzerService.drawAll(msg.source).map(images =>
      images.foreach(image => request(SendPhoto(msg.source, image)).void)
    )
  }


  onCommand("drawall_admin") { implicit msg =>
    priceAnalyzerService.drawAll().map(images =>
      images.foreach(image => request(SendPhoto(msg.source, image)).void)
    )
  }

  onCommand("draw") { implicit msg =>
    withArgs { key =>
      val deviceKey = key.mkString("")
      if (deviceKey.isEmpty) {
        reply(s"${Provider.invalidMessage}\nNo device key passed. Call /devices and copy key after |").void
      } else {
        priceAnalyzerService.draw(msg.source, deviceKey).map({
          case Seq() => reply(s"${Provider.invalidMessage}\nNo devices attached to the key = $deviceKey").void
          case images if images.nonEmpty => images.foreach(image => request(SendPhoto(msg.source, image)).void)
        })
      }
    }
  }
}