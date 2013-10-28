package com.atlassian.ondemand.pageobjects.pages;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.TimedCondition;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;

public class IndraLogoutPage implements Page
{
    private static final String URI = "/logout";

    @ElementBy (id = "logout")
    protected PageElement logoutButton;

    @ElementBy (id = "logged-out")
    protected PageElement loggedOutMessage;


    @Override
    public String getUrl()
    {
        return URI;
    }

    @WaitUntil
    public void doWait()
    {
        waitUntilTrue(isAt());
    }

    public TimedCondition isAt()
    {
        return Conditions.or(logoutButton.timed().isPresent(), loggedOutMessage.timed().isPresent());
    }

    public IndraLogoutPage logout()
    {
        if (loggedOutMessage.isPresent())
        {
            return this;
        }
        else
        {
            logoutButton.click();
        }
        return this;
    }

}

