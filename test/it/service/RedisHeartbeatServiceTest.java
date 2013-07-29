package it.service;

import com.google.common.collect.ImmutableMap;
import com.typesafe.plugin.RedisPlugin;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.Play;
import play.test.FakeApplication;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.*;
import static utils.Constants.VIEWER_EXPIRY_SECONDS;
import static utils.Constants.VIEWER_SET_EXPIRY_SECONDS;
import static util.TimedAsserts.assertStartsPassingAfter;
import static utils.RedisUtils.jedisPool;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import utils.KeyUtils;
import service.RedisHeartbeatService;
import util.FakeHeartbeat;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Heartbeat integration test with Redis. Requires that Redis is up and running locally.
 */
public class RedisHeartbeatServiceTest {
    private static final int VIEWER_EXPIRY_SECONDS_TEST_VALUE = 1;
    private static final int VIEWER_SET_EXPIRY_SECONDS_TEST_VALUE = 2;

    private FakeApplication fakeApplication;
    private RedisHeartbeatService sut;

    @Before
    public void startApp() {

        Map<String, String> fakeConfig = ImmutableMap.of(
                VIEWER_EXPIRY_SECONDS, String.valueOf(VIEWER_EXPIRY_SECONDS_TEST_VALUE),
                VIEWER_SET_EXPIRY_SECONDS, String.valueOf(VIEWER_SET_EXPIRY_SECONDS_TEST_VALUE));

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

        assertStartsPassingAfter(SECONDS.toMillis(VIEWER_EXPIRY_SECONDS_TEST_VALUE), new Callable<Void>() {
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
        assertStartsPassingAfter(SECONDS.toMillis(VIEWER_SET_EXPIRY_SECONDS_TEST_VALUE), new Callable<Void>() {
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

}
