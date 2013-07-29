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
    "commons-io" % "commons-io" % "2.4",
    "com.typesafe" %% "play-plugins-redis" % "2.1-1-RC2-robinf-3",
    "com.atlassian.connect" % "ac-play-java_2.10" % "0.5.4",
    "org.apache.commons" % "commons-lang3" % "3.1",
    "com.google.guava" % "guava" % "14.0.1",
    "commons-codec" % "commons-codec" % "1.8",
    "com.newrelic.agent.java" % "newrelic-api" % "2.20.0"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
        resolvers += "Robin's Maven Repository" at "http://rewbs.bitbucket.org/mavenrepo/releases",  
        resolvers += "org.sedis Maven Repository" at "http://pk11-scratch.googlecode.com/svn/trunk",
        resolvers += "Atlassian's Maven Public Repository" at "https://maven.atlassian.com/content/groups/public",
        resolvers += "Local Maven Repository" at "file://" + Path.userHome + "/.m2/repository",
        scalaVersion := "2.10.0"
  )

}
