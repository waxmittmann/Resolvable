name := "ScalazTaskDoobie"

version := "1.0"

scalaVersion := "2.12.3"

val DoobieVersion = "0.4.1"

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
///home/damxam/.ivy2

libraryDependencies ++=
  List(
    "org.scalaz" %% "scalaz-core" % "7.2.14",
    "org.tpolecat" %% "doobie-core" % DoobieVersion,
    "org.tpolecat" %% "doobie-postgres" % DoobieVersion
  )
