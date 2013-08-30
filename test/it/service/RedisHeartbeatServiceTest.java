package it.service;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.Callable;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.embedded.RedisServer;
import play.test.FakeApplication;
import redis.clients.jedis.Jedis;
import service.RedisHeartbeatService;
import util.FakeHeartbeat;
import utils.KeyUtils;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.start;
import static play.test.Helpers.stop;
import static util.TimedAsserts.assertStartsPassingAfter;
import static utils.Constants.VIEWER_EXPIRY_SECONDS;
import static utils.Constants.VIEWER_SET_EXPIRY_SECONDS;
import static utils.RedisUtils.jedisPool;

/**
 * Heartbeat integration test with Redis. Requires that Redis is up and running locally.
 */
public class RedisHeartbeatServiceTest {
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
    public void startApp() {

        Map<String, String> fakeConfig = ImmutableMap.of(
                VIEWER_EXPIRY_SECONDS, String.valueOf(VIEWER_EXPIRY_SECONDS_TEST_VALUE),
                VIEWER_SET_EXPIRY_SECONDS, String.valueOf(VIEWER_SET_EXPIRY_SECONDS_TEST_VALUE),
                "redis.uri", "redis://localhost:" + redisPort);

        fakeApplication = fakeApplication(fakeConfig);
        start(fakeApplication);

        sut = new RedisHeartbeatService();
    }

    @After
    public void stopApp() {
        stop(fakeApplication);
    }

    @Test
    public void heartbeatShouldPersist() {
        FakeHeartbeat hb = FakeHeartbeat.build();

        sut.put(hb.hostId, hb.resourceId, hb.userId);
        Map<String, String> hbs = sut.list(hb.hostId, hb.resourceId);

        assertTrue("Could not find heartbeat " + hb.userId + " in db. Got: " + hbs, hbs.containsKey(hb.userId));
    }

    @Test
    public void heartbeatShouldExpire() throws Exception {
        final FakeHeartbeat hb = FakeHeartbeat.build();
        sut.put(hb.hostId, hb.resourceId, hb.userId);

        assertStartsPassingAfter(SECONDS.toMillis(VIEWER_EXPIRY_SECONDS_TEST_VALUE*2), new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Map<String, String> hbs = sut.list(hb.hostId, hb.resourceId);
                assertFalse("Unexpected heartbeat " + hb.userId + " in db. Got: " +hbs, hbs.containsKey(hb.userId));
                return null;
            }
        });
    }

    @Test
    public void heartbeatShouldBeDeletable() {
        FakeHeartbeat hb = FakeHeartbeat.build();
        sut.put(hb.hostId, hb.resourceId, hb.userId);
        sut.delete(hb.hostId, hb.resourceId, hb.userId);

        Map<String, String> hbs = sut.list(hb.hostId, hb.resourceId);
        assertFalse("Could not find heartbeat " + hb.userId + " in db. Got: " + hbs, hbs.containsKey(hb.userId));
    }

    @Test
    public void viewerSetShouldExpire() throws Exception {
        FakeHeartbeat hb = FakeHeartbeat.build();
        sut.put(hb.hostId, hb.resourceId, hb.userId);

        final String viewerSetKey = KeyUtils.buildViewerSetKey(hb.hostId, hb.resourceId);
        assertStartsPassingAfter(SECONDS.toMillis(VIEWER_SET_EXPIRY_SECONDS_TEST_VALUE*2), new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                assertRedisKeyAbsent(viewerSetKey);
                return null;
            }
        });
    }

    private static void assertRedisKeyAbsent(String key) {
        Jedis j = jedisPool().getResource();
        try
        {
            assertFalse("Viewer set " + key + " should have expired. You might have a leak.", j.exists(key));
        }
        finally
        {
            jedisPool().returnResource(j);
        }
    }

    private static int findFreePort() throws IOException
    {
        ServerSocket s = new ServerSocket(0);
        int port = s.getLocalPort();
        s.close();
        return port;
    }

}
