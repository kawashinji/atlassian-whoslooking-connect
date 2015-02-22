package controllers;

import java.lang.management.ManagementFactory;
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
import static service.AnalyticsService.ACTIVE_HOST;
import static service.AnalyticsService.ACTIVE_USER;
import static utils.Constants.ANALYTICS_EXPIRY_SECONDS;
import static utils.Constants.ANALYTICS_EXPIRY_SECONDS_DEFAULT;
import static utils.Constants.DISPLAY_NAME_CACHE_EXPIRY_SECONDS;
import static utils.Constants.DISPLAY_NAME_CACHE_EXPIRY_SECONDS_DEFAULT;
import static utils.Constants.ENABLE_DISPLAY_NAME_FETCH;
import static utils.Constants.ENABLE_DISPLAY_NAME_FETCH_BLACKLIST;
import static utils.Constants.ENABLE_METRICS;
import static utils.Constants.POLLER_INTERVAL_SECONDS;
import static utils.Constants.POLLER_INTERVAL_SECONDS_DEFAULT;
import static utils.Constants.VIEWER_EXPIRY_SECONDS;
import static utils.Constants.VIEWER_EXPIRY_SECONDS_DEFAULT;
import static utils.Constants.VIEWER_SET_EXPIRY_SECONDS;
import static utils.Constants.VIEWER_SET_EXPIRY_SECONDS_DEFAULT;

public class Healthcheck  extends Controller
{

    private final AnalyticsService analyticsService = new RedisAnalyticsService();
    private final MetricsService metricsService = new MetricsService();
    
    @play.db.jpa.Transactional
    public Result index() {
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
        final DateTime yesterday = now.minusDays(1);
        final DateTime lastWeek = now.minusDays(7);
        
        analyticsService.gc();
        
        return ImmutableMap.<String, Long>builder()
            .put("dailyActiveUsers", analyticsService.count(ACTIVE_USER, yesterday, now))
            .put("dailyActiveHosts", analyticsService.count(ACTIVE_HOST, yesterday, now))
            .put("weeklyActiveUsers", analyticsService.count(ACTIVE_USER, lastWeek, now))
            .put("weeklyActiveHosts", analyticsService.count(ACTIVE_HOST, lastWeek, now))            
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
                .put(DISPLAY_NAME_CACHE_EXPIRY_SECONDS, conf.getInt(DISPLAY_NAME_CACHE_EXPIRY_SECONDS, DISPLAY_NAME_CACHE_EXPIRY_SECONDS_DEFAULT))
                .put(VIEWER_EXPIRY_SECONDS, conf.getInt(VIEWER_EXPIRY_SECONDS, VIEWER_EXPIRY_SECONDS_DEFAULT))
                .put(VIEWER_SET_EXPIRY_SECONDS, conf.getInt(VIEWER_SET_EXPIRY_SECONDS, VIEWER_SET_EXPIRY_SECONDS_DEFAULT))
                .put(ENABLE_DISPLAY_NAME_FETCH, conf.getBoolean(ENABLE_DISPLAY_NAME_FETCH))
                .put(ENABLE_DISPLAY_NAME_FETCH_BLACKLIST, conf.getBoolean(ENABLE_DISPLAY_NAME_FETCH_BLACKLIST))
                .put(ENABLE_METRICS, conf.getBoolean(ENABLE_METRICS))
                .build();
    }
}
