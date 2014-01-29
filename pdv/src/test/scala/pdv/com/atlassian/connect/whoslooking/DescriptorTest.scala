package pdv.com.atlassian.connect.whoslooking

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.EitherValues
import org.scalatest.matchers.ShouldMatchers

import dispatch.Defaults.executor
import dispatch.Http
import dispatch.as
import dispatch.enrichFuture
import dispatch.implyRequestHandlerTuple
import dispatch.url
import junit.framework.TestCase
import pdv.util.WhosLookingTestConfig
import scala.concurrent.duration.Duration
import scala.concurrent.Await


class DescriptorTest extends ShouldMatchers with EitherValues {

  val Config = WhosLookingTestConfig

  @Test
  def shouldServeDescriptorWithExpectedTopLevelElement() {
    val req = url(Config.WhosLookingBaseUrl) <:< Map("Accept" -> "application/json")
    val descriptorFuture = Http(req OK as.String)
    val descriptorStr = descriptorFuture()
    descriptorStr should be === expectedDescriptorJson
  }

  val expectedDescriptorJson =
    """{
    "key": "whoslooking-connect",
    "name": "Who's Looking for OnDemand",
    "description": "Who&apos;s Looking for OnDemand. See who else is looking at a JIRA issue.",
    "vendor": {
        "name": "Atlassian",
        "url": "http://www.atlassian.com"
    },
    "baseUrl": "http://localhost:9000",
    "authentication": {
        "type": "jwt"
    },
    "lifecycle": {
        "installed": "/installed"
    },
    "modules": {

        "webPanels": [{
            "key" : "whos-looking",
            "name": {
                "value": "Who's Looking?"
            },
            "url": "/poller?issue_id={issue.id}&issue_key={issue.key}",
            "location": "atl.jira.view.issue.right.context"
        }]
    },
    "scopes": ["READ"]
}

"""

}