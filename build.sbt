name := "price-analyzer"

version := "0.1"

scalaVersion := "2.12.10"

libraryDependencies += "com.bot4s" %% "telegram-core" % "4.4.0-RC2"

libraryDependencies += "com.bot4s" %% "telegram-akka" % "4.4.0-RC2"

libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "2.2.0"

libraryDependencies += "com.github.wookietreiber" %% "scala-chart" % "latest.integration"

libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "2.9.0"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.23"

libraryDependencies += "org.json4s" %% "json4s-native" % "3.6.7"

enablePlugins(JavaAppPackaging)