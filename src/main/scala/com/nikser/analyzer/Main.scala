package com.nikser.analyzer

import com.nikser.analyzer.bot.RandomBot

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App {
  val token = scala.util.Properties.envOrElse("TOKEN", "")
  val bot = new RandomBot(token)
  val eol = bot.run()
  Await.result(eol, Duration.Inf)
}
