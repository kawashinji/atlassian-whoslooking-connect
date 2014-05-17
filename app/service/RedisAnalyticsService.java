package service;

import java.util.Set;

import com.newrelic.api.agent.Trace;

import org.joda.time.DateTime;

import play.Logger;
import play.Play;
import play.libs.F.Callback;
import play.libs.F.Function0;
import play.libs.F.Promise;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import static java.util.concurrent.TimeUnit.SECONDS;
import static utils.Constants.ANALYTICS_EXPIRY_SECONDS;
import static utils.Constants.ANALYTICS_EXPIRY_SECONDS_DEFAULT;
import static utils.RedisUtils.jedisPool;

public class RedisAnalyticsService implements AnalyticsService
{

    private final long analyticsExpirySeconds;
    
    private MetricsService metricsService = new MetricsService();

    public RedisAnalyticsService()
    {
        this.analyticsExpirySeconds = Play.application().configuration().getInt(ANALYTICS_EXPIRY_SECONDS, ANALYTICS_EXPIRY_SECONDS_DEFAULT);
    }

    @Override
    public void fire(final String metricName, final String eventKey)
    {
        final long eventTime = System.currentTimeMillis();
        final String metricKey = buildEventKey(metricName);

        Promise<Void> promise = Promise.promise(new Function0<Void>()
        {
            @Override
            @Trace(metricName="write-analytics-event", dispatcher=true)
            public Void apply()
            {
                Jedis j = jedisPool().getResource();
                
                try
                {
                    Transaction t = j.multi();
                    t.sadd("analytics-metrics", metricKey);
                    t.zadd(metricKey, eventTime, eventKey);
                    t.exec();
                }
                finally
                {
                    jedisPool().returnResource(j);
                }

                return null;
            }
        });

        promise.onFailure(new Callback<Throwable>()
        {
            @Override
            @Trace(metricName="write-analytics-events-error", dispatcher=true)
            public void invoke(Throwable e) throws Throwable
            {
                metricsService.incCounter("analytics.fail");
                Logger.warn("Failed to write analitics event " + metricName + "/" + eventKey, e);
            }
        });

        promise.onRedeem(new Callback<Void>()
        {
            @Override
            public void invoke(Void v) throws Throwable
            {
                metricsService.incCounter("analytics.success");
                Logger.trace("Wrote analytics event.");
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
        final long oldestWantedMetric = System.currentTimeMillis() - SECONDS.toMillis(analyticsExpirySeconds);

        Jedis j = jedisPool().getResource();
        try
        {
            Set<String> metricKeys = j.smembers("analytics-metrics");
            Transaction t = j.multi();
            for (String metricKey : metricKeys)
            {
                t.zremrangeByScore(metricKey, 0, oldestWantedMetric);
            }
            t.exec();
        }
        finally
        {
            jedisPool().returnResource(j);
        }
    }

    private static String buildEventKey(String metricsName)
    {
        return "analytics-" + metricsName;
    }

}
