package com.atlassian.connect.whoslooking.pageobjects.pages;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.UriBuilder;

import com.atlassian.jira.pageobjects.pages.viewissue.ViewIssuePage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.AbstractTimedCondition;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.webdriver.waiter.webdriver.AtlassianWebDriverWait;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewIssuePageWithWhosLookingPanel extends ViewIssuePage
{
    private static final Logger log = LoggerFactory.getLogger(ViewIssuePageWithWhosLookingPanel.class);

    private static final By ONLOOKER_SELECTOR = By.cssSelector("#whoslooking-onlookers-list .whoslooking-displayname");
    private static final By IFRAME_SELECTOR = By.xpath("//iframe[contains(@id, 'whos-looking')]");
    private static final By ANON_SELECTOR = By.id("whoslooking-anonymous");
    private static final By ANON_LOGIN_LINK_SELECTOR = By.id("whoslooking-anonymous-login-link");

    @ElementBy(cssSelector = "#issue-panel-whos-looking .ap-loaded")
    private PageElement whosLookingLoadedStatus;

    public ViewIssuePageWithWhosLookingPanel(String issueKey)
    {
        super(issueKey);
    }

    public ViewIssuePageWithWhosLookingPanel(String issueKey, String anchor)
    {
        super(issueKey, anchor);
    }

    @SuppressWarnings("unchecked")
    @Override
    public TimedCondition isAt()
    {
        return Conditions.or(this.isListingViewers(), this.isAnonymous());
    }

    public TimedCondition isAnonymous()
    {
        return new AbstractTimedCondition(TimeUnit.SECONDS.toMillis(10), 200)
        {
            @Override
            protected Boolean currentValue()
            {
                try
                {
                    if (!driver.elementExists(IFRAME_SELECTOR))
                    {
                        return false;
                    }
                    driver.switchTo().frame(driver.findElement(IFRAME_SELECTOR));

                    boolean isAnon = driver.findElements(ANON_SELECTOR).size() > 0;

                    log.debug("Who's looking viewer is anonymous: {}", isAnon);

                    return isAnon;
                }
                finally
                {
                    driver.switchTo().defaultContent();
                }
            }
        };
    }

    public TimedCondition isListingViewers()
    {
        return new AbstractTimedCondition(TimeUnit.SECONDS.toMillis(10), 200)
        {
            @Override
            protected Boolean currentValue()
            {
                return getViewers().size() > 0;
            }
        };
    }

    public Optional<String> getLoginLinkUrl()
    {
        try
        {
            if (!driver.elementExists(IFRAME_SELECTOR))
            {
                return Optional.absent();
            }

            driver.switchTo().frame(driver.findElement(IFRAME_SELECTOR));
            List<WebElement> loginLinkElements = driver.findElements(ANON_LOGIN_LINK_SELECTOR);
            if (loginLinkElements.size() < 1)
            {
                return Optional.absent();
            }
            return Optional.of(loginLinkElements.get(0).getAttribute("href"));
        }
        finally
        {
            driver.switchTo().defaultContent();
        }
    }

    public Optional<String> getWhosLookingBaseUrl()
    {
        try
        {
            if (!driver.elementExists(IFRAME_SELECTOR))
            {
                return Optional.absent();
            }

            String iframeSrc = driver.findElement(IFRAME_SELECTOR).getAttribute("src");
            URI iframeUrl = UriBuilder.fromPath(iframeSrc).build();
            return Optional.of(iframeUrl.getScheme() + "://" + iframeUrl.getHost() + (iframeUrl.getPort() > 0 ? ":" + iframeUrl.getPort() : ""));

        }
        finally
        {
            driver.switchTo().defaultContent();
        }
    }

    public List<String> getViewers()
    {
        try
        {
            if (!driver.elementExists(IFRAME_SELECTOR))
            {
                return ImmutableList.of();
            }

            driver.switchTo().frame(driver.findElement(IFRAME_SELECTOR));

            // Lookup list of names, catering for the fact that the underlying elements might be swiped away by the client code  as we try to read them:
            List<String> viewersNames =  new AtlassianWebDriverWait(driver, 30000, 200)
                .ignoring(StaleElementReferenceException.class)
                .until(new Function<WebDriver, List<String>>() {

                @Override
                public List<String> apply(WebDriver driver)
                {
                    List<WebElement> viewerElements = driver.findElements(ONLOOKER_SELECTOR);
                    List<String> viewersNames = ImmutableList.copyOf(Lists.transform(viewerElements, new Function<WebElement, String>()
                    {
                        @Override
                        public String apply(WebElement input)
                        {
                            return input.getText();
                        }
                    }));

                    return viewersNames;
                }
            });

            log.debug("Found the following viewers on {}: {}", driver.getCurrentUrl(), viewersNames);
            return viewersNames;


        }
        finally
        {
            driver.switchTo().defaultContent();
        }
    }


}
