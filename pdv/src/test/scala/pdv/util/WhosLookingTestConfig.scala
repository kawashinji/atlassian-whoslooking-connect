package pdv.util

import org.slf4j.LoggerFactory
import dispatch._
import dispatch.Defaults._

case class User(name: String, password: String, displayName: String)

object WhosLookingTestConfig {

  val LOG = LoggerFactory.getLogger(WhosLookingTestConfig.getClass.asInstanceOf[Class[Singleton]])

  val WhosLookingBaseUrl = System.getProperty("baseurl.whoslooking", "http://localhost:9000")
  val JiraBaseUrl = System.getProperty("baseurl.jira", "http://localhost:2990/jira")
  val PublicIssueKey = System.getProperty("public.issue.key", "DEMO-1")
  val IsOnDemand = List("jira.com", "atlassian.net", "jira-dev.com") exists { JiraBaseUrl endsWith _ }

  val Admin1 = User(System.getProperty("admin.username", "admin"),
    System.getProperty("admin.password", "admin"),
    System.getProperty("admin.displayname", "Howard Moon"))

  val User1 = User(System.getProperty("user.username", "user"),
    System.getProperty("user.password", "user"),
    System.getProperty("user.displayname", "Vince Noir"))

  LOG.info(s"Config: baseurl.jira=${JiraBaseUrl}, baseurl.whoslooking=${WhosLookingBaseUrl}, public.issue.key=${PublicIssueKey}")
  LOG.info(s"        admin=${Admin1.name}/${Admin1.displayName}, user=${User1.name}/${User1.displayName}")
  sanityCheckConfig

  def sanityCheckConfig {
      // Ping the Connect app. This verifies the URI, and also wakes it up if it has been quiesced
      val req = url(WhosLookingBaseUrl) / "healthcheck"
      val resquestStart = System.currentTimeMillis()
      val response = Http(req > as.lift.Json).map(_ \\ "isHealthy")
      LOG.info("Who's Looking is awake (response time: {}ms). Healthy? {}", System.currentTimeMillis() - resquestStart, response().values)
  }

}