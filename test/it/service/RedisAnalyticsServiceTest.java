package it.service;

import java.util.concurrent.Callable;

import com.google.common.collect.ImmutableMap;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import play.test.FakeApplication;
import service.RedisAnalyticsService;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static util.RedisTestUtils.startNewFakeAppWithRedis;
import static util.RedisTestUtils.stopFakeAppWithRedis;
import static util.TimedAsserts.assertStartsPassingAfter;
import static util.TimedAsserts.assertStartsPassingBefore;
import static utils.Constants.ANALYTICS_EXPIRY_SECONDS;

/**
 * Analytics integration test with Redis. Starts local Redis instances.
 */
public class RedisAnalyticsServiceTest
{
    private static final int ANALYTICS_EXPIRY_SECONDS_TEST_VALUE = 3;
    
    private FakeApplication fakeApp;
    private RedisAnalyticsService sut2;

    @Before
    public void startApp() throws Exception
    {
        fakeApp = startNewFakeAppWithRedis(ImmutableMap.of(ANALYTICS_EXPIRY_SECONDS, String.valueOf(ANALYTICS_EXPIRY_SECONDS_TEST_VALUE)));

        sut2 = new RedisAnalyticsService();
    }

    @After
    public void stopApp()
    {
        stopFakeAppWithRedis(fakeApp);
    }

    @Test
    public void analyticsShouldPersist() throws Exception
    {
        final int numEventsToFire = 1000;
        fireEvents(numEventsToFire);
        
        assertStartsPassingBefore(SECONDS.toMillis(1), new Callable<Void>()
        {
            @Override
            public Void call() throws Exception
            {
                long count = sut2.count("test-metric", DateTime.now().minusMinutes(1), DateTime.now());
                assertEquals("Unexpected count. Got: " + count, numEventsToFire, count);
                return null;
            }
        });
    }

    @Test
    public void analyticsShouldExpire() throws Exception
    {
        final int numEventsToFire = 1000;
        fireEvents(numEventsToFire);

        assertStartsPassingAfter(SECONDS.toMillis(ANALYTICS_EXPIRY_SECONDS_TEST_VALUE * 2), new Callable<Void>()
        {
            @Override
            public Void call() throws Exception
            {
                sut2.gc();
                long count = sut2.count("test-metric", DateTime.now().minusMinutes(1), DateTime.now());
                assertEquals("Unexpected count. Got: " + count, 0, count);
                return null;
            }
        });
    }
    
    private void fireEvents(final int numEventsToFire)
    {
        for (int i=0; i<numEventsToFire; i++)
        {
            sut2.fire("test-metric", "key-" + i);
        }
    }
    
}
