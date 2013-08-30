package it.controller;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import play.mvc.Result;

import controllers.Viewers;
import play.test.FakeApplication;
import redis.embedded.RedisServer;
import service.RedisHeartbeatService;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.start;
import static play.test.Helpers.stop;
import static utils.Constants.VIEWER_EXPIRY_SECONDS;
import static utils.Constants.VIEWER_SET_EXPIRY_SECONDS;

import static play.test.Helpers.*;

/**
 * Heartbeat integration test with Redis. Starts a local Redis instance.
 */
public class ViewerTest
{
    private static final int VIEWER_EXPIRY_SECONDS_TEST_VALUE = 1;
    private static final int VIEWER_SET_EXPIRY_SECONDS_TEST_VALUE = 2;

    private FakeApplication fakeApplication;
    private RedisHeartbeatService sut;
    private static RedisServer redisServer;
    private static int redisPort;

    @BeforeClass
    public static void startRedis() throws Exception
    {
        redisPort = findFreePort();
        redisServer = new RedisServer(redisPort);
        redisServer.start();
    }

    @AfterClass
    public static void stopRedis() throws Exception
    {
        redisServer.stop();
    }

    @Before
    public void startApp()
    {

        Map<String, String> fakeConfig = ImmutableMap.of(VIEWER_EXPIRY_SECONDS,
                                                         String.valueOf(VIEWER_EXPIRY_SECONDS_TEST_VALUE),
                                                         VIEWER_SET_EXPIRY_SECONDS,
                                                         String.valueOf(VIEWER_SET_EXPIRY_SECONDS_TEST_VALUE),
                                                         "redis.uri",
                                                         "redis://localhost:" + redisPort);

        fakeApplication = fakeApplication(fakeConfig);
        start(fakeApplication);

        sut = new RedisHeartbeatService();
    }

    @After
    public void stopApp()
    {
        stop(fakeApplication);
    }

    @Test
    public void shouldRejectPutIfTokenIsInvalid()
    {
        Result result = route(fakeRequest("PUT", "/viewables/some-host/some-resource/viewers/some-user"));
        System.out.println(status(result));
    }


    private static int findFreePort() throws IOException
    {
        ServerSocket s = new ServerSocket(0);
        int port = s.getLocalPort();
        s.close();
        return port;
    }

}
