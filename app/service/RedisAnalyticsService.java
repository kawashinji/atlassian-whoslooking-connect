package service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;

import play.Play;
import play.libs.F.Function0;
import play.libs.F.Promise;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import static utils.Constants.ANALYTICS_EXPIRY_DAYS;
import static utils.Constants.ANALYTICS_EXPIRY_DAYS_DEFAULT;
import static utils.RedisUtils.jedisPool;

public class RedisAnalyticsService implements AnalyticsService
{

    private final long analyticsExpiryDays;

    public RedisAnalyticsService()
    {
        this.analyticsExpiryDays = Play.application().configuration()
                                       .getInt(ANALYTICS_EXPIRY_DAYS, ANALYTICS_EXPIRY_DAYS_DEFAULT);
    }

    @Override
    public void fire(final String eventName, final String eventData)
    {
        final long eventTime = System.currentTimeMillis();
        final String eventKey = buildEventKey(eventName);

        Promise.promise(new Function0<Void>()
        {
            @Override
            public Void apply()
            {
                Jedis j = jedisPool().getResource();
                try
                {
                    Transaction t = j.multi();
                    t.sadd("analytics-event-keys", eventKey);
                    t.zadd(eventKey, eventTime, eventData);
                    t.exec();
                }
                finally
                {
                    jedisPool().returnResource(j);
                }

                return null;
            }
        });
    }

    @Override
    public long count(String eventName, DateTime start, DateTime end)
    {
        Jedis j = jedisPool().getResource();
        final String eventKey = buildEventKey(eventName);
        try
        {
            return j.zcount(eventKey, start.getMillis(), end.getMillis());
        }
        finally
        {
            jedisPool().returnResource(j);
        }
    }

    @Override
    public void gc()
    {
        final long nowMs = System.currentTimeMillis();

        Jedis j = jedisPool().getResource();
        try
        {
            Set<String> eventKeys = j.smembers("analytics-event-keys");
            Transaction t = j.multi();
            for (String eventKey : eventKeys)
            {
                t.zremrangeByScore(eventKey, 0, nowMs - TimeUnit.DAYS.toMillis(analyticsExpiryDays));
            }
            t.exec();
        }
        finally
        {
            jedisPool().returnResource(j);
        }
    }

    private static String buildEventKey(String eventName)
    {
        return "analytics-" + eventName;
    }

}
