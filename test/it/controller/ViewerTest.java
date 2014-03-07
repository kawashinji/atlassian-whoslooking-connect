package it.controller;

import java.util.Map;
import java.util.Set;

import com.atlassian.connect.play.java.token.Token;
import com.atlassian.fugue.Option;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import play.api.libs.Crypto;
import play.libs.Json;
import play.mvc.Result;
import play.test.FakeApplication;
import play.test.FakeRequest;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static play.test.Helpers.status;
import static util.RedisTestUtils.startNewFakeAppWithRedis;
import static util.RedisTestUtils.stopFakeAppWithRedis;
import static utils.Constants.PER_PAGE_VIEW_TOKEN_HEADER;

/**
 * Viewer controller test. Starts a local Redis instance.
 */
public class ViewerTest
{
    private static final String TEST_RESOURCE_ID = "test-resource-id";
    private static final String TEST_HOST_ID = "test-host-1";

    private FakeApplication fakeApp;

    @Before
    public void startApp() throws Exception
    {
        fakeApp = startNewFakeAppWithRedis();
        registerHost(TEST_HOST_ID);
    }

    @After
    public void stopApp()
    {
        stopFakeAppWithRedis(fakeApp);
    }

    @Test
    public void shouldRejectPutIfTokenIsMissing()
    {
        Result result = route(putViewer("some-user"));    
        assertEquals(HTTP_UNAUTHORIZED, status(result));
    }
    
    @Test
    public void shouldRejectPutIfTokenIsInvalid()
    {
        Result result = route(putViewer("some-user").withHeader(PER_PAGE_VIEW_TOKEN_HEADER, "garbage"));   
        assertEquals(HTTP_UNAUTHORIZED, status(result));
    }
    
    @Test
    public void shouldRejectDeleteIfTokenIsMissing()
    {
        Result result = route(deleteViewer("some-user"));    
        assertEquals(HTTP_UNAUTHORIZED, status(result));
    }
    
    @Test
    public void shouldRejectDeleteIfTokenIsInvalid()
    {
        Result result = route(deleteViewer("some-user").withHeader(PER_PAGE_VIEW_TOKEN_HEADER, "garbage"));   
        assertEquals(HTTP_UNAUTHORIZED, status(result));
    }    

    @Test
    public void shouldAcceptPutIfTokenIsValid()
    {
        Result result = routeAndExpectSuccess(putViewerWithValidToken("some-user"));   
        assertEquals(ImmutableSet.of("some-user"), extractViewers(result));
    }

    @Test
    public void shouldTrackMultipleViewers()
    {
        routeAndExpectSuccess(putViewerWithValidToken("some-user-1"));
        Result result = routeAndExpectSuccess(putViewerWithValidToken("some-user-2"));

        assertEquals(ImmutableSet.of("some-user-1", "some-user-2"), extractViewers(result));
    }
    
    @Test
    public void shouldNotListDeletedViewer()
    {
        routeAndExpectSuccess(putViewerWithValidToken("some-user-1"));
        routeAndExpectSuccess(putViewerWithValidToken("some-user-2"));
        routeAndExpectSuccess(deleteViewerWithValidToken("some-user-2"));
        Result result = routeAndExpectSuccess(putViewerWithValidToken("some-user-1"));

        assertEquals(ImmutableSet.of("some-user-1"), extractViewers(result));
    }
    
    @Test
    public void shouldNotConflateViewersOfDifferentResources()
    {
        routeAndExpectSuccess(putViewerWithValidToken(TEST_HOST_ID, "resource-1", "some-user-1"));
        Result result = routeAndExpectSuccess(putViewerWithValidToken(TEST_HOST_ID, "resource-2", "some-user-2"));

        assertEquals(ImmutableSet.of("some-user-2"), extractViewers(result));
    }
    
    @Test
    public void shouldNotConflateViewersOfDifferentHosts()
    {
        final String TEST_HOST_2_ID = "test-host-2";        
        registerHost(TEST_HOST_2_ID);
        
        routeAndExpectSuccess(putViewerWithValidToken(TEST_HOST_ID, TEST_RESOURCE_ID, "some-user-1"));        
        Result result = routeAndExpectSuccess(putViewerWithValidToken(TEST_HOST_2_ID, TEST_RESOURCE_ID, "some-user-2"));
        
        assertEquals(ImmutableSet.of("some-user-2"), extractViewers(result));
    }
    
    public static Result routeAndExpectSuccess(FakeRequest req)
    {
        Result result = route(req);
        int status = status(result);
        assertTrue(String.format("Unexpected status %s. Body: %s", status, contentAsString(result)),
                   HTTP_OK == status || HTTP_NO_CONTENT == status);
        return result;
    }
    
    private static Set<String> extractViewers(Result result)
    {
        return ImmutableSet.copyOf(Json.parse(contentAsString(result)).fieldNames());
    }

    private static FakeRequest putViewer(String user)
    {
        return putViewer(TEST_HOST_ID, TEST_RESOURCE_ID, user);
    }
    
    private static FakeRequest putViewer(String host, String resource, String user)
    {
        return fakeRequest("PUT", viewerUriFor(host, resource, user));
    }

    private static FakeRequest putViewerWithValidToken(String user)
    {
        return putViewerWithValidToken(TEST_HOST_ID, TEST_RESOURCE_ID, user);
    }
    
    private static FakeRequest putViewerWithValidToken(String host, String resource, String user)
    {
        return putViewer(host, resource, user).withHeader(PER_PAGE_VIEW_TOKEN_HEADER, tokenFor(host, user));
    }

    private static FakeRequest deleteViewer(String user)
    {
        return fakeRequest("DELETE", viewerUriFor(TEST_HOST_ID, TEST_RESOURCE_ID, user));
    }

    private static FakeRequest deleteViewerWithValidToken(String user)
    {
        return deleteViewer(user).withHeader(PER_PAGE_VIEW_TOKEN_HEADER, tokenFor(TEST_HOST_ID, user));
    }

    private static String tokenFor(String host, String user)
    {
        final Token token = new Token(host, Option.some(user), System.currentTimeMillis(), true);
        final String jsonToken = Base64.encodeBase64String(token.toJson().toString().getBytes());
        final String encryptedToken = Crypto.encryptAES(jsonToken);
        return encryptedToken;
    }

    private static String viewerUriFor(String host, String resource, String user)
    {
        return "/viewables/" + host + "/" + resource + "/viewers/" + user + "?ac-host=" + TEST_HOST_ID;
    }
    
    private static void registerHost(String key)
    {
        Map<String, String> host = ImmutableMap.of("clientKey", key,
                                                   "baseUrl", "http://ac-mock-tenants.herokuapp.com/jira/" + key,
                                                   "productType", "jira",
                                                   "sharedSecret", "xyz",
                                                   "publicKey", "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCJwaijNZVbGHO/rO58TRSwAbwu0XTQJxHHkDFR1U0OM8VDoHLn/42UBg+tW8GobG6xrPBwINL8MyNYULgzv1NYx6a5HTom3hFA3pBkmi1fy6vFVIE+M3gYA2+A514A227MUZlBIzSr2jldmP+EFYh0J449f+BbWM7VJpZqqV+4WQIDAQAB");
        routeAndExpectSuccess(fakeRequest("PUT", "/installed").withJsonBody(Json.toJson(host)));
    }

}
