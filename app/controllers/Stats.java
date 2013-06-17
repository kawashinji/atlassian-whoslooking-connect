package controllers;

import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import play.mvc.Controller;
import play.mvc.Result;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.atlassian.connect.play.java.AC;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.typesafe.plugin.RedisPlugin;

public class Stats extends Controller
{
    private JedisPool jedisPool()
    {
        return play.Play.application().plugin(RedisPlugin.class).jedisPool();
    }
    
    public Result index() throws UnknownHostException
    {
        String redisStats = getRedisStats();
        String dynoName = System.getenv("PS1");
        String hostname = InetAddress.getLocalHost().getCanonicalHostName();
        Runtime runtime = Runtime.getRuntime();
        Map<String, String> memoryStats = getMemoryStats(runtime);
        return ok(views.html.stats.render(dynoName, hostname, redisStats, System.getenv()));
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
