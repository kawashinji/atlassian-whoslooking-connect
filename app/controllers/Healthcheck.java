package controllers;

import java.lang.management.ManagementFactory;
import java.util.LongSummaryStatistics;
import java.util.Map;

import com.atlassian.connect.play.java.AC;

import com.google.common.collect.ImmutableMap;

import org.apache.commons.lang3.tuple.Pair;
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

        long dailyActiveUsers = analyticsService.count(ACTIVE_USER, yesterday, now);
        long dailyActiveHosts = analyticsService.count(ACTIVE_HOST, yesterday, now);
        long weeklyActiveUsers = analyticsService.count(ACTIVE_USER, lastWeek, now);
        long weeklyActiveHosts = analyticsService.count(ACTIVE_HOST, lastWeek, now);

        Pair<LongSummaryStatistics, Map<Integer, Integer>> perTenantPageStats = analyticsService.getStats(PER_TENANT_PAGE_LOADS, aMinuteAgo, now);
        Pair<LongSummaryStatistics, Map<Integer, Integer>>  perTenantPollStats = analyticsService.getStats(PER_TENANT_POLL_EVENTS, aMinuteAgo, now);

        Map<Integer, Integer> perTenantPageHistogram = perTenantPageStats.getRight();
        Map<Integer, Integer> perTenantPollHistogram = perTenantPollStats.getRight();
        LongSummaryStatistics perTenantPageSummary = perTenantPageStats.getLeft();
        LongSummaryStatistics perTenantPollSummary = perTenantPollStats.getLeft();

        metricsService.sendToHostedGraphite(Pair.of("stats-dailyActiveUsers", dailyActiveUsers),
            Pair.of("stats-dailyActiveHosts", dailyActiveHosts),
            Pair.of("stats-weeklyActiveUsers", weeklyActiveUsers),
            Pair.of("stats-weeklyActiveHosts", weeklyActiveHosts),
            Pair.of("stats-perTenant-pageViewsInLastMinute-max", perTenantPageSummary.getMax()),
            Pair.of("stats-perTenant-pageViewsInLastMinute-avg", Math.round(perTenantPageSummary.getAverage())),
            Pair.of("stats-perTenant-pageViewsInLastMinute-sum", perTenantPageSummary.getSum()),
            Pair.of("stats-perTenant-pageViewsInLastMinute-count", perTenantPageSummary.getCount()),
            Pair.of("stats-perTenant-pollsInLastMinute-max", perTenantPollSummary.getMax()),
            Pair.of("stats-perTenant-pollsInLastMinute-avg", Math.round(perTenantPollSummary.getAverage())),
            Pair.of("stats-perTenant-pollsInLastMinute-sum", perTenantPollSummary.getSum()),
            Pair.of("stats-perTenant-pollsInLastMinute-count", perTenantPollSummary.getCount()));

        Logger.trace("Page view histogram: {}, Poll histogram: {}",perTenantPageHistogram, perTenantPollHistogram);

        Pair[] stats1 = perTenantPageHistogram.entrySet().stream()
                .map((entry) -> Pair.of("stats-perTenant-pageHistogram-upTo" + entry.getKey(), entry.getValue()))
                .toArray(Pair[]::new);
        metricsService.sendToHostedGraphite(stats1);

        Pair[] stats2 = perTenantPollHistogram.entrySet().stream()
                .map((entry) -> Pair.of("stats-perTenant-pollHistogram-upTo" + entry.getKey(), entry.getValue()))
                .toArray(Pair[]::new);
        metricsService.sendToHostedGraphite(stats2);

        Logger.trace("Page view histogram: {}, Poll histogram: {}",stats1, stats2);

        return ImmutableMap.<String, Long>builder()
                .put("dailyActiveUsers", dailyActiveUsers)
                .put("dailyActiveHosts", dailyActiveHosts)
                .put("weeklyActiveUsers", weeklyActiveUsers)
                .put("weeklyActiveHosts", weeklyActiveHosts)
                .put("perTenant-pageViewsInLastMinute-max", perTenantPageSummary.getMax())
                .put("perTenant-pageViewsInLastMinute-avg", Math.round(perTenantPageSummary.getAverage()))
                .put("perTenant-pageViewsInLastMinute-sum", perTenantPageSummary.getSum())
                .put("perTenant-pageViewsInLastMinute-count", perTenantPageSummary.getCount())
                .put("perTenant-pollsInLastMinute-max", perTenantPollSummary.getMax())
                .put("perTenant-pollsInLastMinute-avg", Math.round(perTenantPollSummary.getAverage()))
                .put("perTenant-pollsInLastMinute-sum", perTenantPollSummary.getSum())
                .put("perTenant-pollsInLastMinute-count", perTenantPollSummary.getCount())
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
