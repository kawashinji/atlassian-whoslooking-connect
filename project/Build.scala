import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "whoslooking-remote"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    javaCore,
    javaJdbc,
    javaEbean,
    "com.atlassian.connect" % "ac-play-java_2.10" % "0.5",
    "com.google.code.gson" % "gson" % "2.2.4"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
        resolvers += "Atlassian's Maven Public Repository" at "https://maven.atlassian.com/content/groups/public",
        resolvers += "Local Maven Repository" at "file://" + Path.userHome + "/.m2/repository"
  )

}
