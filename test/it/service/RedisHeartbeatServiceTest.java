package it.service;

import java.util.Map;
import java.util.concurrent.Callable;

import com.google.common.collect.ImmutableMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import play.test.FakeApplication;
import service.RedisHeartbeatService;
import util.FakeHeartbeat;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static util.RedisTestUtils.startNewFakeAppWithRedis;
import static util.RedisTestUtils.stopFakeAppWithRedis;
import static util.TimedAsserts.assertStartsPassingAfter;
import static utils.Constants.VIEWER_EXPIRY_SECONDS;
import static utils.Constants.VIEWER_SET_EXPIRY_SECONDS;

/**
 * Heartbeat integration test with Redis. Starts local Redis instances.
 */
public class RedisHeartbeatServiceTest
{
    private static final int VIEWER_EXPIRY_SECONDS_TEST_VALUE = 1;
    private static final int VIEWER_SET_EXPIRY_SECONDS_TEST_VALUE = 2;
    
    private FakeApplication fakeApp;
    private RedisHeartbeatService sut;

    @Before
    public void startApp() throws Exception
    {
        fakeApp = startNewFakeAppWithRedis(ImmutableMap.of(VIEWER_EXPIRY_SECONDS, String.valueOf(VIEWER_EXPIRY_SECONDS_TEST_VALUE),
                                                           VIEWER_SET_EXPIRY_SECONDS, String.valueOf(VIEWER_SET_EXPIRY_SECONDS_TEST_VALUE)));        
        sut = new RedisHeartbeatService();
    }

    @After
    public void stopApp()
    {
        stopFakeAppWithRedis(fakeApp);
    }

    @Test
    public void heartbeatShouldPersist()
    {
        FakeHeartbeat hb = FakeHeartbeat.build();

        sut.put(hb.hostId, hb.resourceId, hb.userId);
        Map<String, String> hbs = sut.list(hb.hostId, hb.resourceId);

        assertTrue("Could not find heartbeat " + hb.userId + " in db. Got: " + hbs, hbs.containsKey(hb.userId));
    }

    @Test
    public void heartbeatShouldExpire() throws Exception
    {
        final FakeHeartbeat hb = FakeHeartbeat.build();
        sut.put(hb.hostId, hb.resourceId, hb.userId);

        assertStartsPassingAfter(SECONDS.toMillis(VIEWER_EXPIRY_SECONDS_TEST_VALUE * 2), new Callable<Void>()
        {
            @Override
            public Void call() throws Exception
            {
                Map<String, String> hbs = sut.list(hb.hostId, hb.resourceId);
                assertFalse("Unexpected heartbeat " + hb.userId + " in db. Got: " + hbs, hbs.containsKey(hb.userId));
                return null;
            }
        });
    }

    @Test
    public void heartbeatShouldBeDeletable()
    {
        FakeHeartbeat hb = FakeHeartbeat.build();
        sut.put(hb.hostId, hb.resourceId, hb.userId);
        sut.delete(hb.hostId, hb.resourceId, hb.userId);

        Map<String, String> hbs = sut.list(hb.hostId, hb.resourceId);
        assertFalse("Could not find heartbeat " + hb.userId + " in db. Got: " + hbs, hbs.containsKey(hb.userId));
    }
    
}
