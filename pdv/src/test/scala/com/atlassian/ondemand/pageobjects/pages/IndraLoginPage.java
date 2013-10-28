package com.atlassian.ondemand.pageobjects.pages;

import javax.inject.Inject;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.pageobjects.page.LoginPage;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;

public class IndraLoginPage implements LoginPage
{
    private static final String DEFAULT_URI = "/login";

    private static final String REDIRECTING_URI_TEMPLATE = DEFAULT_URI + "?dest-url=%s";

    private final String uri;

    @Inject
    protected PageBinder pageBinder;

    @Inject
    protected PageElementFinder elementFinder;

    @ElementBy(id = "username")
    protected PageElement usernameField;

    @ElementBy(id = "password")
    protected PageElement passwordField;

    @ElementBy(id = "login")
    protected PageElement loginButton;

    public IndraLoginPage()
    {
        this.uri = DEFAULT_URI;
    }

    public IndraLoginPage(final String destPath)
    {
        this.uri = String.format(REDIRECTING_URI_TEMPLATE, destPath);
    }

    @Override
    public String getUrl()
    {
        return uri;
    }

    @WaitUntil
    public void doWait()
    {
        waitUntilTrue(isAt());
    }

    public TimedCondition isAt()
    {
        return loginButton.timed().isPresent();
    }

    public <M extends Page> M login(String username, String password, Class<M> nextPage, Object... args)
    {
        usernameField.clear();
        usernameField.type(username);
        passwordField.clear();
        passwordField.type(password);

        loginButton.click();
        return pageBinder.bind(nextPage, args);
    }

    @Override
    public <M extends Page> M login(String username, String password, Class<M> nextPage)
    {
        return login(username, password, nextPage, new Object[] {});
    }

    @Override
    public <M extends Page> M loginAsSysAdmin(Class<M> nextPage)
    {
        throw new UnsupportedOperationException();
    }

}
