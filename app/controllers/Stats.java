package controllers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import play.mvc.Controller;
import play.mvc.Result;
import redis.clients.jedis.Jedis;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;
import static utils.RedisUtils.jedisPool;

public class Stats extends Controller
{

    @play.db.jpa.Transactional
    public Result index() throws UnknownHostException
    {
        String redisStats = getRedisStats();
        String dynoName = defaultIfEmpty(System.getenv().get("DYNO"), "unknown");
        String hostname = InetAddress.getLocalHost().getCanonicalHostName();
        Runtime runtime = Runtime.getRuntime();
        Map<String, String> memoryStats = getMemoryStats(runtime);
        return ok(views.html.stats.render(dynoName, hostname, redisStats, memoryStats));
    }

    private ImmutableMap<String, String> getMemoryStats(Runtime runtime)
    {
        return ImmutableMap.of("Free memory", byteCountToDisplaySize(runtime.freeMemory()),
                                                          "Total memory", byteCountToDisplaySize(runtime.totalMemory()),
                                                          "Max memory",byteCountToDisplaySize(runtime.totalMemory()));
    }

    private String getRedisStats()
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

}
