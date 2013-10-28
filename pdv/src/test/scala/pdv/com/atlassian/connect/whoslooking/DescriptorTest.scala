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


class DescriptorTest extends ShouldMatchers with EitherValues {

  val Config = WhosLookingTestConfig

  @Test
  def shouldServeDescriptorWithExpectedTopLevelElement() {
    val req = url(Config.WhosLookingBaseUrl) <:< Map("Accept" -> "application/xml")
    val descriptor = Http(req OK as.xml.Elem)
    descriptor().label should be === "atlassian-plugin"
  }

}