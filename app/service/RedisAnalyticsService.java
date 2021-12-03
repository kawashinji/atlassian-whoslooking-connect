package service;

import java.util.*;
import java.util.stream.Collectors;

import com.google.common.base.Supplier;
import org.joda.time.DateTime;

import play.Logger;
import play.Play;
import play.libs.F.Callback;
import play.libs.F.Function0;
import play.libs.F.Promise;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import static java.util.concurrent.TimeUnit.SECONDS;
import static utils.Constants.*;
import static utils.RedisUtils.jedisPool;

public class RedisAnalyticsService implements AnalyticsService
{

    public static final String ANALYTICS_METRICS_SHORTLIVED_KEYS = "analytics-metrics-shortlived-keys";
    public static final String ANALYTICS_METRICS_KEYS = "analytics-metrics-keys";
    private final int analyticsExpirySeconds;
    private final int analyticsExpiryShortLivedSeconds;

    private final MetricsService metricsService = new MetricsService();

    public RedisAnalyticsService()
    {
        this.analyticsExpirySeconds = Play.application().configuration().getInt(ANALYTICS_EXPIRY_SECONDS, ANALYTICS_EXPIRY_SECONDS_DEFAULT);
        this.analyticsExpiryShortLivedSeconds = Play.application().configuration().getInt(ANALYTICS_EXPIRY_SECONDS_SHORTLIVED, ANALYTICS_EXPIRY_SECONDS_SHORTLIVED_DEFAULT);
    }

    @Override
    public void fire(final String metricName, final String eventKey)
    {
        fire(metricName, eventKey, false);
    }

    @Override
    public void fireShortLived(final String metricName, final String eventKey)
    {
        fire(metricName, eventKey, true);
    }

    private void fire(final String metricName, final String eventKey, final boolean isShortLived)
    {
        final long eventTime = System.currentTimeMillis();
        final String metricKey = buildEventKey(metricName);

        Promise<Void> promise = Promise.promise(new Function0<Void>()
        {
            @Override
            public Void apply()
            {
                Jedis j = jedisPool().getResource();

                try
                {
                    Transaction t = j.multi();
                    // Unfortunately jedis uses commons-pool2 from 2.3 onwards, which clashes with Play 2.2.
                    // This means we can't upgrade jedis - so, we can't use the scan API.
                    // We therefore maintain our own index of analytics keys.
                    t.zadd(isShortLived ? ANALYTICS_METRICS_SHORTLIVED_KEYS : ANALYTICS_METRICS_KEYS, eventTime, metricKey);
                    t.zadd(metricKey, eventTime, eventKey);
                    t.expire(metricKey, isShortLived ? analyticsExpiryShortLivedSeconds : analyticsExpirySeconds);
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
            public void invoke(Throwable e) throws Throwable
            {
                metricsService.incCounter("analytics.fail");
                Logger.warn("Failed to write analytics event " + metricName + "/" + eventKey, e);
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
        metricsService.withMetric("analytics-gc-timer", new Supplier<Void>() {

            @Override
            public Void get() {
                final long now = System.currentTimeMillis();
                final long oldestWantedMetric = now - SECONDS.toMillis(analyticsExpirySeconds);
                final long oldestWantedMetricShortLived = now - SECONDS.toMillis(analyticsExpiryShortLivedSeconds);

                Jedis j = jedisPool().getResource();
                try {
                    // Using a self-maintained index of keys because we can't upgrade jedis to be able to use scan.
                    j.zrange(ANALYTICS_METRICS_KEYS, 0, -1).forEach(k -> j.zremrangeByScore(k, 0, oldestWantedMetric));
                    j.zrange(ANALYTICS_METRICS_SHORTLIVED_KEYS, 0, -1).forEach(k -> j.zremrangeByScore(k, 0, oldestWantedMetricShortLived));
                } finally {
                    jedisPool().returnResource(j);
                }

                return null;
            }

        });

    }

    @Override
    public LongSummaryStatistics getStats(final String category, DateTime start, DateTime end) {
        Jedis j = jedisPool().getResource();
        try
        {
            return getPerTenantKeys(category).stream()
                    .mapToLong(s -> {
                        long count = j.zcount(s, start.getMillis(), end.getMillis());
                        Logger.trace("Got {} elements for {} in {}", count, s, category);
                        return count;
                    })
                    .summaryStatistics();
        }
        finally
        {
            jedisPool().returnResource(j);
        }
    }


    private static Set<String> getPerTenantKeys(final String category)
    {
        Logger.trace("Scanning for keys in category: {}", category);

        Jedis j = jedisPool().getResource();
        final String prefix = buildEventKey(category);
        try
        {
            Set<String> allKeys = j.zrange(ANALYTICS_METRICS_SHORTLIVED_KEYS, 0, -1).stream()
                    .filter(k -> k.startsWith(prefix))
                    .collect(Collectors.toSet());
            Logger.trace("Scan found these keys: {}", allKeys);
            return  allKeys;
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
