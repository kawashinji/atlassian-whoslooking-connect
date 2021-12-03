package controllers;

import java.lang.management.ManagementFactory;
import java.util.LongSummaryStatistics;
import java.util.Map;

import com.atlassian.connect.play.java.AC;

import com.google.common.collect.ImmutableMap;

import org.joda.time.DateTime;

import play.Configuration;
import play.Logger;
import play.Play;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import redis.clients.jedis.exceptions.JedisException;
import service.AnalyticsService;
import service.MetricsService;
import service.RedisAnalyticsService;
import utils.VersionUtils;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static service.AnalyticsService.*;
import static utils.Constants.*;

public class Healthcheck  extends Controller
{

    private final AnalyticsService analyticsService = new RedisAnalyticsService();
    private final MetricsService metricsService = new MetricsService();

    @play.db.jpa.Transactional
    public Result index() {
        metricsService.incCounter("page-hit.healthcheck");
        try
        {
            Map<String, Long> activity = getActivity();
            return ok(Json.toJson(
                    ImmutableMap.builder()
                            .putAll(basicHealthInfo())
                            .put("activity", activity)
                            .put("config", configInfo())
                            .put("metrics", metricsService.getAllSamples())
                            .put("isHealthy", true)
                            .build()
            ));
        }
        catch(JedisException e)
        {
            Logger.error("Health check failed to access Redis", e);
            return status(HTTP_UNAVAILABLE, Json.toJson(
                            ImmutableMap.builder()
                                .putAll(basicHealthInfo())
                                .put("isHealthy", false)
                                .put("failureReason", "Health check failed to access Redis" + e.getMessage())
                                .build()
            ));
        }
    }

    private Map<String, Long> getActivity()
    {
        final DateTime now = DateTime.now();
        final DateTime aMinuteAgo = now.minusMinutes(1);
        final DateTime yesterday = now.minusDays(1);
        final DateTime lastWeek = now.minusDays(7);

        analyticsService.gc();

        LongSummaryStatistics perTenantPageStats = analyticsService.getStats(PER_TENANT_PAGE_LOADS, aMinuteAgo, now);
        LongSummaryStatistics perTenantPollStats = analyticsService.getStats(PER_TENANT_POLL_EVENTS, aMinuteAgo, now);

        long dailyActiveUsers = analyticsService.count(ACTIVE_USER, yesterday, now);
        long dailyActiveHosts = analyticsService.count(ACTIVE_HOST, yesterday, now);
        long weeklyActiveUsers = analyticsService.count(ACTIVE_USER, lastWeek, now);
        long weeklyActiveHosts = analyticsService.count(ACTIVE_HOST, lastWeek, now);

        metricsService.sendToHostedGraphite("stats-dailyActiveUsers", dailyActiveUsers);
        metricsService.sendToHostedGraphite("stats-dailyActiveHosts", dailyActiveHosts);
        metricsService.sendToHostedGraphite("stats-weeklyActiveUsers", weeklyActiveUsers);
        metricsService.sendToHostedGraphite("stats-weeklyActiveHosts", weeklyActiveHosts);

        metricsService.sendToHostedGraphite("stats-perTenant-pageViewsInLastMinute-max", perTenantPageStats.getMax());
        metricsService.sendToHostedGraphite("stats-perTenant-pageViewsInLastMinute-avg", Math.round(perTenantPageStats.getAverage()));
        metricsService.sendToHostedGraphite("stats-perTenant-pageViewsInLastMinute-sum", perTenantPageStats.getSum());
        metricsService.sendToHostedGraphite("stats-perTenant-pageViewsInLastMinute-count", perTenantPageStats.getCount());

        metricsService.sendToHostedGraphite("stats-perTenant-pollsInLastMinute-max", perTenantPollStats.getMax());
        metricsService.sendToHostedGraphite("stats-perTenant-pollsInLastMinute-avg", Math.round(perTenantPollStats.getAverage()));
        metricsService.sendToHostedGraphite("stats-perTenant-pollsInLastMinute-sum", perTenantPollStats.getSum());
        metricsService.sendToHostedGraphite("stats-perTenant-pollsInLastMinute-count", perTenantPollStats.getCount());

        return ImmutableMap.<String, Long>builder()
                .put("dailyActiveUsers", dailyActiveUsers)
                .put("dailyActiveHosts", dailyActiveHosts)
                .put("weeklyActiveUsers", weeklyActiveUsers)
                .put("weeklyActiveHosts", weeklyActiveHosts)
                .put("perTenant-pageViewsInLastMinute-max", perTenantPageStats.getMax())
                .put("perTenant-pageViewsInLastMinute-avg", Math.round(perTenantPageStats.getAverage()))
                .put("perTenant-pageViewsInLastMinute-sum", perTenantPageStats.getSum())
                .put("perTenant-pageViewsInLastMinute-count", perTenantPageStats.getCount())
                .put("perTenant-pollsInLastMinute-max", perTenantPollStats.getMax())
                .put("perTenant-pollsInLastMinute-avg", Math.round(perTenantPollStats.getAverage()))
                .put("perTenant-pollsInLastMinute-sum", perTenantPollStats.getSum())
                .put("perTenant-pollsInLastMinute-count", perTenantPollStats.getCount())
                .build();

    }

    private Map<String, Object> basicHealthInfo() {
        return ImmutableMap.<String, Object>builder()
                .put("name", AC.PLUGIN_NAME)
                .put("key", AC.PLUGIN_KEY)
                .put("version", VersionUtils.VERSION)
                .put("dyno", defaultIfEmpty(System.getenv().get("DYNO"), "unknown"))
                .put("time", System.currentTimeMillis())
                .put("freeMemory", Runtime.getRuntime().freeMemory())
                .put("systemLoad", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage())
                .build();
    }

    private Map<String, Object> configInfo() {
        Configuration conf = Play.application().configuration();
        return ImmutableMap.<String, Object>builder()
                .put(POLLER_INTERVAL_SECONDS, conf.getInt(POLLER_INTERVAL_SECONDS, POLLER_INTERVAL_SECONDS_DEFAULT))
                .put(ANALYTICS_EXPIRY_SECONDS, conf.getInt(ANALYTICS_EXPIRY_SECONDS, ANALYTICS_EXPIRY_SECONDS_DEFAULT))
                .put(ANALYTICS_EXPIRY_SECONDS_SHORTLIVED, conf.getInt(ANALYTICS_EXPIRY_SECONDS_SHORTLIVED, ANALYTICS_EXPIRY_SECONDS_SHORTLIVED_DEFAULT))
                .put(DISPLAY_NAME_CACHE_EXPIRY_SECONDS, conf.getInt(DISPLAY_NAME_CACHE_EXPIRY_SECONDS, DISPLAY_NAME_CACHE_EXPIRY_SECONDS_DEFAULT))
                .put(VIEWER_EXPIRY_SECONDS, conf.getInt(VIEWER_EXPIRY_SECONDS, VIEWER_EXPIRY_SECONDS_DEFAULT))
                .put(VIEWER_SET_EXPIRY_SECONDS, conf.getInt(VIEWER_SET_EXPIRY_SECONDS, VIEWER_SET_EXPIRY_SECONDS_DEFAULT))
                .put(ENABLE_DISPLAY_NAME_FETCH, conf.getBoolean(ENABLE_DISPLAY_NAME_FETCH))
                .put(ENABLE_DISPLAY_NAME_FETCH_BLACKLIST, conf.getBoolean(ENABLE_DISPLAY_NAME_FETCH_BLACKLIST))
                .put(ENABLE_METRICS, conf.getBoolean(ENABLE_METRICS))
                .build();
    }
}
