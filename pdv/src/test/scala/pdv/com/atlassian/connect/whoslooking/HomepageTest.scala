package pdv.com.atlassian.connect.whoslooking

import org.junit.Test
import dispatch._
import Defaults._
import pdv.util.WhosLookingTestConfig
import pdv.util.TimedAssertions
import org.scalatest.junit.ShouldMatchersForJUnit
import org.scalatest.EitherValues
import junit.framework.TestCase

class HomepageTest extends ShouldMatchersForJUnit with EitherValues {

  val Config = WhosLookingTestConfig

  @Test
  def shouldServeHtmlHomepageWithSensibleTitle() {
    val req = url(Config.WhosLookingBaseUrl)
    val title = Http(req > as.jsoup.Document).map(_.title())
    title() should include("Who's Looking")
  }

}