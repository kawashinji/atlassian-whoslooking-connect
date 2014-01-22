import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "whoslooking-connect"
  val appVersion      = "2.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    javaCore,
    javaJdbc,
    javaEbean,
    "commons-io" % "commons-io" % "2.4",
    "com.typesafe" %% "play-plugins-redis" % "2.1-1-RC2-robinf-3",
    "com.atlassian.connect" % "ac-play-java_2.10" % "0.7.0-BETA-SNAPSHOT" withSources(),
    "org.apache.commons" % "commons-lang3" % "3.1",
    "com.google.guava" % "guava" % "14.0.1",
    "commons-codec" % "commons-codec" % "1.8",
    "com.newrelic.agent.java" % "newrelic-api" % "2.20.0",
    "redis.embedded" % "embedded-redis" % "0.1" % "test" exclude("com.google.guava", "guava-io")
//    "com.atlassian.jira" % "atlassian-jira-pageobjects" % "6.1-OD-06" % "test" exclude("com.google.collections", "google-collections")  exclude("org.apache.ws.commons", "XmlSchema")     
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
        resolvers += "Robin's Maven Repository" at "http://rewbs.bitbucket.org/mavenrepo/releases",  
        resolvers += "org.sedis Maven Repository" at "http://pk11-scratch.googlecode.com/svn/trunk",
        resolvers += "Atlassian's Maven Public Repository" at "https://maven.atlassian.com/content/groups/public",
        resolvers += "Local Maven Repository" at "file://" + Path.userHome + "/.m2/repository",
        resolvers += "clojars.org" at "http://clojars.org/repo",

        scalaVersion := "2.10.0",

        testOptions in Test ~= { args =>
          for {
            arg <- args
            val ta: Tests.Argument = arg.asInstanceOf[Tests.Argument]
            val newArg = if(ta.framework == Some(TestFrameworks.JUnit)) ta.copy(args = List.empty[String]) else ta
          } yield newArg
        }
  )

}
