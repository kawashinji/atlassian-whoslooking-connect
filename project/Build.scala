import sbt._
import Keys._
import play.Project._

import scala.collection.generic.SeqForwarder
import scala.collection.{LinearSeq, SeqProxy, SeqViewLike, immutable, mutable}

object ApplicationBuild extends Build {

  val appName         = "whoslooking-connect"
  val appVersion      = "2.0.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    javaCore,
    javaJdbc,
    javaEbean,
    "commons-io" % "commons-io" % "2.4",
    // Unfortunately jedis uses commons-pool2 from 2.3 onwards, which clashes with Play 2.2.
    // This means we can't upgrade jedis (so, for example, we can't use the scan API).
    // The best solution would be to upgrade Play, but this doesn't seem worth it given this app
    // will soon be migrated to Forge.
    "redis.clients" % "jedis" % "2.2.0",
    "com.typesafe" %% "play-plugins-redis" % "2.2.1",
    "com.atlassian.connect" % "ac-play-java_2.10" % "0.10.4-robinf" withSources(),
    "org.apache.commons" % "commons-lang3" % "3.1",
    "org.apache.commons" % "commons-text" % "1.9",
    "com.squareup.okhttp3" % "okhttp" % "4.9.2",
    "com.google.guava" % "guava" % "14.0.1",
    "commons-codec" % "commons-codec" % "1.10",
    "org.javasimon" % "javasimon-core" % "4.0.1",
    "redis.embedded" % "embedded-redis" % "0.1" % "test" exclude("com.google.guava", "guava-io")
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
        resolvers += "Robin's Maven Repository" at "https://rewbs.bitbucket.io/mavenrepo/releases",
        resolvers += "Atlassian's Maven Public Repository" at "https://maven.atlassian.com/content/groups/public",
        resolvers += "Local Maven Repository" at "file://" + Path.userHome + "/.m2/repository",
        resolvers += "clojars.org" at "http://clojars.org/repo",

        scalaVersion := "2.10.4",

        testOptions in Test ~= { args =>
          for {
            arg <- args
            val ta: Tests.Argument = arg.asInstanceOf[Tests.Argument]
            val newArg = if(ta.framework == Some(TestFrameworks.JUnit)) ta.copy(args = List.empty[String]) else ta
          } yield newArg
        }
  )

}
