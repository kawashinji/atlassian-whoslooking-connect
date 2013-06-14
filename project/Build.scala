import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "whoslooking-connect"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    javaCore,
    javaJdbc,
    javaEbean,
    "com.typesafe" %% "play-plugins-redis" % "2.1-1-RC2-robinf",
    "com.atlassian.connect" % "ac-play-java_2.10" % "0.5.1"
    
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
        resolvers += "org.sedis Maven Repository" at "http://pk11-scratch.googlecode.com/svn/trunk",
        resolvers += "Atlassian's Maven Public Repository" at "https://maven.atlassian.com/content/groups/public",
        resolvers += "Local Maven Repository" at "file://" + Path.userHome + "/.m2/repository",
        scalaVersion := "2.10.0"
  )

}
