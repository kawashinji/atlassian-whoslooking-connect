package controllers;

import com.atlassian.connect.play.java.AC;
import com.atlassian.connect.play.java.controllers.AcController;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import java.io.IOException;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import static org.apache.commons.lang.StringUtils.split;
import static utils.RedisUtils.jedisPool;

public class Healthcheck  extends Controller
{
    public Result index() {
        try
        {
            Map<String, Object> redisHealthInfo = redisHealthInfo();
            return ok(Json.toJson(
                    ImmutableMap.builder()
                            .putAll(basicHealthInfo())
                            .put("redisStatus", redisHealthInfo)
                            .put("isHealthy", true)
                            .build()
            ));
        }
        catch(JedisException e)
        {
            Logger.error("Health check failed to access Redis", e);
            Map<String, Object> redisHealthInfo = redisHealthInfo();
            return ok(Json.toJson(
                    ImmutableMap.builder()
                            .putAll(basicHealthInfo())
                            .put("isHealthy", false)
                            .put("failureReason", "Health check failed to access Redis" + e.getMessage())
                            .build()
            ));
        }
    }

    private Map<String, Object> basicHealthInfo() {
        return ImmutableMap.<String, Object>builder()
                .put("name", "Who's Looking Connect")
                .put("description", "Who's Looking on Dyno " + System.getenv().get("PS"))
                .put("time", System.currentTimeMillis())
                .put("freeMemory", Runtime.getRuntime().freeMemory())
                .put("systemLoad", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage())
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
        for (String section: split(rawStatsString, "#")) {
            Scanner scanner = new Scanner(section);

            String sectionTitle = scanner.nextLine().trim();
            Map<String, String> sectionEntries = new LinkedHashMap<String, String>();
            redisStats.put(sectionTitle, sectionEntries);

            while (scanner.hasNextLine()) {
                String[] kvp = split(scanner.nextLine(), ":");
                if (kvp.length > 1) {
                    sectionEntries.put(kvp[0], kvp[1]);
                }
            }
        }

        return redisStats;
    }
}
