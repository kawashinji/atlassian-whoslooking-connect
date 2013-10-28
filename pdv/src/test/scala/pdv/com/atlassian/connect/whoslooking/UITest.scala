package pdv.com.atlassian.connect.whoslooking

import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.scalatest.EitherValues
import org.scalatest.junit.ShouldMatchersForJUnit
import org.slf4j.LoggerFactory
import com.atlassian.connect.whoslooking.pageobjects.pages.ViewIssuePageWithWhosLookingPanel
import com.atlassian.jira.pageobjects.JiraTestedProduct
import com.atlassian.jira.pageobjects.pages.DashboardPage
import com.atlassian.ondemand.pageobjects.pages.IndraLoginPage
import com.atlassian.ondemand.pageobjects.pages.IndraLogoutPage
import com.atlassian.pageobjects.TestedProductFactory
import com.atlassian.pageobjects.TestedProductFactory.TesterFactory
import com.atlassian.pageobjects.elements.timeout.TimeoutType
import com.atlassian.webdriver.WebDriverFactory
import com.atlassian.webdriver.pageobjects.DefaultWebDriverTester
import pdv.util.TimedAssertions
import pdv.util.User
import pdv.util.WhosLookingTestConfig
import org.junit.Ignore

/**
 * Runs browser tests against a JIRA instance with Who's Looking installed.
 * The JIRA instance may be a JIRA standalone instance (e.g. for local testing) or a JIRA OnDemand instance (e.g. for staging or production post-deployment verification).
 *
 * Assumptions:
 * <ul>
 *   <li>Who's Looking is installed on the instance.</li>
 *   <li>Who's Looking is running at ${baseurl.whoslooking:-'http://localhost:9000'}</li>
 *   <li>Instance available at ${baseurl.jira:-'http://localhost:2990/jira'}</li>
 *   <li>Instance has the following publicly visible issue: ${public.issue.key:-'DEMO-1'}</li>
 *   <li>Instance has the following admin ${admin.username:-'admin'} /  ${admin.password:-'admin'} /  ${admin.displayname:-'Howard Moon'}</li>
 *   <li>Instance has the following user ${user.username:-'user'} /  ${user.password:-'user'} /  ${user.displayname:-'Vince Noir'}</li>
 * </ul>
 */
class UITest extends TimedAssertions with ShouldMatchersForJUnit with EitherValues {

  val LOG = LoggerFactory.getLogger(classOf[ViewIssuePageWithWhosLookingPanel])
  val Config = WhosLookingTestConfig
  val product = TestedProductFactory.create(classOf[JiraTestedProduct])

  @Rule
  def screenShotOnFailure() = new TestWatcher() {
    override def failed(t: Throwable, test: Description) = {
      val sourceFile = new File("test-output", test.getMethodName() + ".html")
      val screenshotFile = new File("test-output", test.getMethodName() + ".png")
      product.getTester().getDriver().dumpSourceTo(sourceFile)
      product.getTester().getDriver().takeScreenshotTo(screenshotFile)
      LOG.info("Test failure detected, dumped source to {} and screenshot {}", sourceFile.getAbsolutePath(), screenshotFile.getAbsolutePath())
    }
  }

  @Test
  def shouldBeUsingExpectedWhosLookingInstance() {
    logout()
    val page = goToIssuePage()

    page.getWhosLookingBaseUrl().get() shouldEventually be === Config.WhosLookingBaseUrl
  }

  @Test
  def shouldListNoViewersAndPromptLoginWithCorrectUrlIfUserIsAnonymous() {
    logout()
    val page = goToIssuePage()

    page.getViewers.size() shouldEventually be === 0
    page.getLoginLinkUrl().get() shouldEventually endWith("/login.jsp?os_destination=%2Fbrowse%2F" + Config.PublicIssueKey)
  }

  @Test
  def shouldListAuthenticatedViewerWithCorrectDisplayName() {
    loginAs(Config.Admin1)
    val page = goToIssuePage()

    page.getViewers shouldEventually (contain(Config.Admin1.displayName), TimeoutType.SLOW_PAGE_LOAD)
  }

  @Test
  def shouldListMultipleViewersWithCorrectDisplayName() {
    loginAs(Config.Admin1)
    val page = goToIssuePage()

    // TODO: clean up multi-session hackery
    val secondDriver = WebDriverFactory.getDriver()
    try {
      val secondProduct = TestedProductFactory.create(classOf[JiraTestedProduct], "jira", new TesterFactory[DefaultWebDriverTester]() {
        def create() = new DefaultWebDriverTester(secondDriver)
      })
      loginAs(Config.User1, secondProduct)
      goToIssuePage(secondProduct)

      page.getViewers shouldEventually (contain(Config.Admin1.displayName), TimeoutType.SLOW_PAGE_LOAD)
      page.getViewers shouldEventually (contain(Config.User1.displayName), TimeoutType.SLOW_PAGE_LOAD)
    } finally {
      secondDriver.quit();
    }
  }

  @Test
  @Ignore("TimeoutType.SLOW_PAGE_LOAD is not long enough for the viewer to expire.")
  def shouldExpireNonViewer() {
    loginAs(Config.Admin1)
    val page = goToIssuePage()

    // TODO: clean up multi-session hackery
    val secondDriver = WebDriverFactory.getDriver()
    try {
      val secondProduct = TestedProductFactory.create(classOf[JiraTestedProduct], "jira", new TesterFactory[DefaultWebDriverTester]() {
        def create() = new DefaultWebDriverTester(secondDriver)
      })
      loginAs(Config.User1, secondProduct)
      goToIssuePage(secondProduct)

      // Wait for both users to be visible
      page.getViewers shouldEventually (contain(Config.Admin1.displayName), TimeoutType.SLOW_PAGE_LOAD)
      page.getViewers shouldEventually (contain(Config.User1.displayName), TimeoutType.SLOW_PAGE_LOAD)

      logout(secondProduct)

      page.getViewers shouldEventually (contain(Config.Admin1.displayName), TimeoutType.SLOW_PAGE_LOAD)
      // User1 should eventually drop off. If this fails, perhaps another session (unrelated to the test) is using User1?
      page.getViewers shouldEventually (not contain(Config.User1.displayName), TimeoutType.SLOW_PAGE_LOAD)
    } finally {
      secondDriver.quit();
    }
  }

  def loginAs(user: User, withProduct: JiraTestedProduct = product) = {
    logout(withProduct)
    if (Config.IsOnDemand) {
      withProduct.visit(classOf[IndraLoginPage], "/login").login(user.name, user.password, classOf[IndraLoginPage])
    } else {
      withProduct.gotoLoginPage().login(user.name, user.password, classOf[DashboardPage])
    }
  }

  def goToIssuePage(withProduct: JiraTestedProduct = product) = withProduct.visit(classOf[ViewIssuePageWithWhosLookingPanel], Config.PublicIssueKey)

  def logout(withProduct: JiraTestedProduct = product) = {
    if (Config.IsOnDemand) {
      withProduct.visit(classOf[IndraLogoutPage]).logout()
    } else {
      withProduct.logout
    }
  }

}