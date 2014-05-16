package controllers;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import com.atlassian.connect.play.java.AC;
import com.atlassian.connect.play.java.model.AcHostModel;

import com.google.common.collect.ImmutableMap;

import org.javasimon.SimonManager;
import org.joda.time.DateTime;

import static service.AnalyticsService.ACTIVE_USER_V2;
import static service.AnalyticsService.ACTIVE_HOST_V2;
import play.Logger;
import play.Play;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;
import service.AnalyticsService;
import service.RedisAnalyticsService;
import utils.VersionUtils;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.split;
import static service.AnalyticsService.ACTIVE_HOST;
import static service.AnalyticsService.ACTIVE_USER;
import static utils.Constants.POLLER_INTERVAL_SECONDS;
import static utils.Constants.POLLER_INTERVAL_SECONDS_DEFAULT;
import static utils.RedisUtils.jedisPool;

public class Healthcheck  extends Controller
{

    private final AnalyticsService analyticsService = new RedisAnalyticsService();
    
    @play.db.jpa.Transactional
    public Result index() {
        try
        {
            Map<String, Long> activity = getActivity();
            return ok(Json.toJson(
                    ImmutableMap.builder()
                            .putAll(basicHealthInfo())
                            .put("activity", activity)
                            .put("poller.db", SimonManager.getStopwatch("poller.db").sample())
                            .put("poller.hb", SimonManager.getStopwatch("poller.hb").sample())
                            .put("poller.a", SimonManager.getStopwatch("poller.a").sample())
                            .put("poller.v", SimonManager.getStopwatch("poller.v").sample())
                            .put("poller.r", SimonManager.getStopwatch("poller.r").sample())
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
            .put("dailyV2ActiveUsers", analyticsService.count(ACTIVE_USER_V2, yesterday, now))
            .put("dailyV2ActiveHosts", analyticsService.count(ACTIVE_HOST_V2, yesterday, now))
            .put("weeklyV2ActiveUsers", analyticsService.count(ACTIVE_USER_V2, lastWeek, now))
            .put("weeklyV2ActiveHosts", analyticsService.count(ACTIVE_HOST_V2, lastWeek, now))            
            .build();
    }

    private Map<String, Object> basicHealthInfo() {
        return ImmutableMap.<String, Object>builder()
                .put("name", AC.PLUGIN_NAME)
                .put("key", AC.PLUGIN_KEY)
                .put("version", VersionUtils.VERSION)
                .put("dyno", defaultIfEmpty(System.getenv().get("DYNO"), "unknown"))
                .put("hosts", AcHostModel.all().size())
                .put("time", System.currentTimeMillis())
                .put("freeMemory", Runtime.getRuntime().freeMemory())
                .put("systemLoad", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage())
                .put(POLLER_INTERVAL_SECONDS, Play.application().configuration().getInt(POLLER_INTERVAL_SECONDS, POLLER_INTERVAL_SECONDS_DEFAULT))
                .build();
    }

    private Map<String, Object> redisHealthInfo() {
        ImmutableMap.<String, Object>builder();
        String rawStatsString = redisStats();
        return parseRawRedisStats(rawStatsString);
    }

    private String redisStats()
    {
        String redisStats;
        Jedis j = jedisPool().getResource();
        try
        {
            redisStats = j.info();
        }
        finally
        {
            jedisPool().returnResource(j);
        }
        return redisStats;
    }

    private Map<String, Object> parseRawRedisStats(String rawStatsString)
    {
        Map<String, Object> redisStats = new LinkedHashMap<String, Object>();
        for (String section : split(rawStatsString, "#"))
        {
            try (Scanner scanner = new Scanner(section))
            {

                String sectionTitle = scanner.nextLine().trim();
                Map<String, String> sectionEntries = new LinkedHashMap<String, String>();
                redisStats.put(sectionTitle, sectionEntries);

                while (scanner.hasNextLine())
                {
                    String[] kvp = split(scanner.nextLine(), ":");
                    if (kvp.length > 1)
                    {
                        sectionEntries.put(kvp[0], kvp[1]);
                    }
                }
            }
        }

        return redisStats;
    }
}
