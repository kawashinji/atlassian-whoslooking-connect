package pdv.com.atlassian.connect.whoslooking

import org.junit.Test
import dispatch._
import Defaults._
import pdv.util.WhosLookingTestConfig
import pdv.util.TimedAssertions
import org.scalatest.junit.ShouldMatchersForJUnit
import org.scalatest.EitherValues
import junit.framework.TestCase

class HealthCheckTest extends ShouldMatchersForJUnit with EitherValues {

  val Config = WhosLookingTestConfig

  @Test
  def shouldPassHealthCheck() {
    val req = url(Config.WhosLookingBaseUrl) / "healthcheck"
    val isHealthy = Http(req > as.lift.Json).map(_ \\ "isHealthy")
    isHealthy().values should be === true
  }

}