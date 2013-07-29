package utils;

import com.typesafe.plugin.RedisPlugin;
import redis.clients.jedis.JedisPool;

public class RedisUtils
{
    public static JedisPool jedisPool()
    {
        return play.Play.application().plugin(RedisPlugin.class).jedisPool();
    }
}
