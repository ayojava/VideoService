name := """VideoServer-Play-Scala-Slick-NoAuth"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  // jdbc,    // conflicts with slick
  cache,
  ws,
  specs2 % Test,
  // "com.typesafe.slick" %% "slick" % "3.0.3",   // this library is included as a transitive dependency of "play-slick"
  "com.typesafe.play" %% "play-slick" % "1.0.1",
  "com.typesafe.play" %% "play-slick-evolutions" % "1.0.1",    // has evolutions as a transitive dependency
  "com.h2database" % "h2" % "1.3.176"     // required as play-slick doesn't include the JDBC driver
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
