package pdv;

import org.junit.Test;

/**
 * Runs Who's Looking post-deployment verification tests against a JIRA instance with Who's Looking installed. Assumptions:
 * Who's Looking is installed on the instance.
 * Instance available at ${sut.baseurl:-'http://localhost:2990/jira'}
 * Instance has the following publicly visible issue: ${issue.key:-'DEMO-1'}
 * Instance has the following admin ${admin.username:-'admin'} /  ${admin.password:-'admin'} /  ${admin.fullname:-'Howard Moon'}
 * Instance has the following user ${user.username:-'user'} /  ${user.password:-'user'} /  ${user.fullname:-'Vince Noir'}
 */
public class WhosLookingSpec
{


    @Test
    public void shouldPromptLoginIfUserIsAnonymous()
    {
    }

    @Test
    public void shouldDisplayCurrentUserFullName()
    {

    }

    @Test
    public void shouldListTwoViewers()
    {

    }

    @Test
    public void shouldExpireNonViewer()
    {

    }

    @Test
    public void shouldPassHealthCheck()
    {

    }
    
    @Test
    public void shouldServeDescriptor()
    {

    }

    @Test
    public void shouldServeHomepage()
    {

    }
}
