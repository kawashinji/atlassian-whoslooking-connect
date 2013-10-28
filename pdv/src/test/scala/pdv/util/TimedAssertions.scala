package pdv.util

import org.scalatest.matchers.{MatchResult, Matcher}
import com.atlassian.pageobjects.elements.timeout.{TimeoutType, Timeouts}
import com.atlassian.pageobjects.elements.query._
import com.atlassian.pageobjects.util.InjectingTestedProducts
import org.scalatest.junit.{ShouldMatchersForJUnit, JUnitTestFailedError}
import com.atlassian.pageobjects.elements.timeout.Timeouts
import com.atlassian.pageobjects.TestedProduct

/**
 * Provides a DSL for timed assertions using Atlassian Selenium.
 * (Via Luis / Service Desk)
 */
trait TimedAssertions extends ShouldMatchersForJUnit {
  self: {def product: TestedProduct[_]} =>

  /**
   * Gets the Timeouts from the product.
   */
  private lazy val timeouts = InjectingTestedProducts.asInjectionContext(product).getInstance(classOf[Timeouts])

  /**
   * Wraps a Scala lazy function argument into a TimedQueryWrapper. This is what makes the magic possible.
   */
  implicit def convertToTimedQuery[T](query: => T): QueryShouldEventuallyWrapper[T] = new QueryShouldEventuallyWrapper[T](query)

  /**
   * Wrapper for a query (lazy function) that we wish to perform assertions on.
   */
  class QueryShouldEventuallyWrapper[T](query: => T) {
    /**
     * Implementation of "shouldEventually".
     *
     * @param rightMatcher a Matcher[T]
     * @param withTimeout how long to wait before giving up (default: TimeoutType.DEFAULT)
     */
    def shouldEventually(rightMatcher: Matcher[T], withTimeout: TimeoutType = TimeoutType.DEFAULT) {
      val timeoutAfter = timeouts.timeoutFor(withTimeout)
      val matchesCondition = new AbstractTimedCondition(timeoutAfter, PollingQuery.DEFAULT_INTERVAL) {
        var lastResult: MatchResult = null
        def currentValue() = {
          lastResult = rightMatcher.apply(query)
          lastResult.matches
        }
      }

      if (!matchesCondition.by(timeoutAfter)) {
        throw failedException(matchesCondition.lastResult.failureMessage)
      }
    }
  }

  /**
   * Fails a JUnit test, fixing up the stack trace.
   */
  private def failedException(message: String, optionalCause: Option[Throwable] = None): Throwable = {
    val fileNames = List("TimedAssertions.scala")
    val temp = new RuntimeException
    val stackDepth = temp.getStackTrace.takeWhile(stackTraceElement => fileNames.exists(_ == stackTraceElement.getFileName) || stackTraceElement.getMethodName == "newTestFailedException").length

    optionalCause match {
      case Some(cause) => new JUnitTestFailedError(message, cause, stackDepth)
      case None => new JUnitTestFailedError(message, stackDepth)
    }
  }
}
