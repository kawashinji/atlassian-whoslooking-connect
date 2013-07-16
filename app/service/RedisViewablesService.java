package service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;
import com.typesafe.plugin.RedisPlugin;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import play.Play;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

/**
* Provides an implementation of ViewablesService backed by Redis.
*/
public class RedisViewablesService implements ViewablesService
{

    // A viewer is considered to not be looking anymore if no heartbeat has been received in this amount for time.
    private static final int VIEWER_EXPIRY_SECONDS = Play.application().configuration().getInt("whoslooking.viewer-expiry.seconds", 10);

    // A viewer set associated with an issue is purged if no one has requested it for this amount of time.
    private static final int VIEWER_SET_EXPIRY_SECONDS = Play.application().configuration().getInt("whoslooking.viewer-set-expiry.seconds", (int)TimeUnit.DAYS.toSeconds(1));

    @Override
    public Map<String, String> getViewers(final String hostId, final String resourceId)
    {

        final String resourceKey = buildResourceKey(hostId, resourceId);
        Jedis j = jedisPool().getResource();
        Map<String, String> activeViewers = Maps.newHashMap();
        try
        {
            Set<String> allViewers  = j.smembers(resourceKey);
            j.expire(resourceKey, VIEWER_SET_EXPIRY_SECONDS);

            // Filter out expired viewers
            for (String viewerKey : allViewers)
            {
                String lastSeen = j.get(viewerKey);
                if (StringUtils.isBlank(lastSeen))
                {
                    // Viewer has expired, remove them from resource set.
                    j.srem(resourceKey, viewerKey);
                }
                else
                {
                    // Transform ["host-resource-user"] into ["user"]
                    String userName = viewerKey.substring(viewerKey.lastIndexOf('-') + 1);
                    activeViewers.put(userName, lastSeen);
                }
            }
        }
        finally
        {
            jedisPool().returnResource(j);
        }

        return activeViewers;
    }

    @Override
    public void putViewer(final String hostId, final String resourceId, final String newViewer)
    {
        final String viewerKey = buildViewerKey(hostId, resourceId, newViewer);
        final String resourceKey = buildResourceKey(hostId, resourceId);

        Jedis j = jedisPool().getResource();
        try
        {
            Transaction t = j.multi();
            t.sadd(resourceKey, viewerKey);
            t.set(viewerKey, String.valueOf(System.currentTimeMillis()));
            t.expire(viewerKey, VIEWER_EXPIRY_SECONDS);
            t.exec();
        }
        finally
        {
            jedisPool().returnResource(j);
        }
    }

    @Override
    public void deleteViewer(final String hostId, final String resourceId, final String viewer)
    {
        String key = buildViewerKey(hostId, resourceId, viewer);
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
        Map<String, JsonNode> viewersWithDetails = Maps.transformEntries(this.getViewers(hostId, resourceId), new Maps.EntryTransformer<String, String, JsonNode>()
        {
            @Override
            public JsonNode transformEntry(String username, String lastSeen)
            {
                ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
                objectNode.put("displayName", ViewerDetailsService.getCachedDisplayNameFor(hostId, username));
                objectNode.put("lastSeen", lastSeen);
                return objectNode;
            }
        });
        return viewersWithDetails;
    }

    private JedisPool jedisPool()
    {
        return play.Play.application().plugin(RedisPlugin.class).jedisPool();
    }

    private String buildViewerKey(final String hostId, final String resourceId, final String userId)
    {
        return hostId + '-' + resourceId + '-' +  userId;
    }

    private String buildResourceKey(final String hostId, final String resourceId)
    {
        return hostId + '-' + resourceId;
    }
}
