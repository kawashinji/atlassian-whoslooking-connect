package pdv.com.atlassian.connect.whoslooking

import org.junit.Test
import org.scalatest.EitherValues
import org.scalatest.matchers.ShouldMatchers

import dispatch.Defaults.executor
import dispatch.Http
import dispatch.as
import dispatch.enrichFuture
import dispatch.implyRequestHandlerTuple
import dispatch.url
import pdv.util.WhosLookingTestConfig


class DescriptorTest extends ShouldMatchers with EitherValues {

  val Config = WhosLookingTestConfig

  @Test
  def shouldServeDescriptorWithExpectedTopLevelElement() {
    val req = url(Config.WhosLookingBaseUrl) <:< Map("Accept" -> "application/json")
    val descriptorFuture = Http(req OK as.String)
    val descriptorStr = descriptorFuture()
    // TODO: make comparison more comprehensive. Note some fields change per env and this test is run on diff envs
    // so can't do a full json comparison
    descriptorStr should include (""""name": "Who's Looking for OnDemand"""")
  }
}