package com.nikser.analyzer.parser

import com.nikser.analyzer.config.Provider
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.{attr, text}
import net.ruippeixotog.scalascraper.dsl.DSL._

class OnlinerParser(browser: Browser = Provider.browser) {

  def parseName(url: String): String = {
    val doc = browser.get(url)
    doc >> attr("content")("meta[property=og:title]")
  }

  def parsePrice(url: String): String = {
    val doc = browser.get(url)
    doc >> text("span[itemprop=lowPrice]")
  }

  def parseKey(url: String): String = {
    val index = url.lastIndexOf("/")
    url.substring(index + 1)
  }

}
