package service;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.typesafe.plugin.RedisPlugin;

import org.codehaus.jackson.JsonNode;

import play.Play;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
* Provides an implementation of ViewablesService backed by Redis.
*/
public class RedisViewablesService implements ViewablesService
{

    private static final int VIEWER_EXPIRY_SECONDS = Play.application()
                                                                   .configuration()
                                                                   .getInt("whoslooking.viewer-expiry.seconds", 10);

    @Override
    public Set<String> getViewers(final String hostId, final String resourceId)
    {

        String pattern = hostId + '-' + resourceId + "-*";
        Jedis j = jedisPool().getResource();
        Set<String> keySet;
        try
        {
            keySet = j.keys(pattern);
        }
        finally
        {
            jedisPool().returnResource(j);
        }

        // Transform ["host-resource-user"] into ["user"]
        return ImmutableSet.copyOf(Collections2.transform(keySet, new Function<String, String>()
        {
            @Override
            public String apply(String key)
            {
                return key.substring(key.lastIndexOf('-') + 1);
            }

        }));
    }

    @Override
    public  void putViewer(final String hostId, final String resourceId, final String newViewer)
    {
        String key = buildKey(hostId, resourceId, newViewer);

        Jedis j = jedisPool().getResource();
        try
        {
            j.set(key, String.valueOf(System.currentTimeMillis()));
            j.expire(key, VIEWER_EXPIRY_SECONDS);
        }
        finally
        {
            jedisPool().returnResource(j);
        }
    }

    @Override
    public void deleteViewer(final String hostId, final String resourceId, final String viewer)
    {
        String key = buildKey(hostId, resourceId, viewer);
        Jedis j = jedisPool().getResource();
        try
        {
            j.del(key);
        }
        finally
        {
            jedisPool().returnResource(j);
        }
    }

    @Override
    public Map<String, JsonNode> getViewersWithDetails(final String resourceId, final String hostId)
    {
        Map<String, JsonNode> viewersWithDetails = Maps.asMap(this.getViewers(hostId, resourceId),
                                                              new Function<String, JsonNode>()
                                                              {
                                                                  @Override
                                                                  @Nullable
                                                                  public JsonNode apply(@Nullable String viewerName)
                                                                  {
                                                                      return ViewerDetailsService.getCachedDetailsFor(hostId, viewerName);
                                                                  }
                                                              });
        return viewersWithDetails;
    }

    private JedisPool jedisPool()
    {
        return play.Play.application().plugin(RedisPlugin.class).jedisPool();
    }

    private String buildKey(final String hostId, final String resourceId, final String userId)
    {
        return hostId + '-' + resourceId + '-' +  userId;
    }
}
