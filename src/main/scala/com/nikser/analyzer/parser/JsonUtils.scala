package com.nikser.analyzer.parser

import org.json4s._
import org.json4s.native.JsonMethods._

object JsonUtils {

  def parseJson(jsonString: String): JValue = {
    parse(jsonString)
  }

  def toMapStringAny(json: JValue): Map[String, Any] = {
    json.values.asInstanceOf[Map[String, Any]]
  }

}
