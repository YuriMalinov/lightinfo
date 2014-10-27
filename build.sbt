name := """lightinfo"""

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  "org.squeryl" %% "squeryl" % "0.9.6-RC3",
  "org.webjars" %% "webjars-play" % "2.2.2-1",
  "org.webjars" % "bootstrap" % "2.3.1",
  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
  "org.postgresql" % "postgresql" % "9.3-1102-jdbc4",
  "org.json4s" %% "json4s-native" % "3.2.10",
  "ws.securesocial" %% "securesocial" % "2.1.4"
)

resolvers += Resolver.sonatypeRepo("releases")

play.Project.playScalaSettings