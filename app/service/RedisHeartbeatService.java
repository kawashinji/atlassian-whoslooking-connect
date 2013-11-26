package service;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

import org.apache.commons.lang3.StringUtils;

import play.Play;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import static utils.Constants.VIEWER_EXPIRY_SECONDS;
import static utils.Constants.VIEWER_EXPIRY_SECONDS_DEFAULT;
import static utils.Constants.VIEWER_SET_EXPIRY_SECONDS;
import static utils.Constants.VIEWER_SET_EXPIRY_SECONDS_DEFAULT;
import static utils.KeyUtils.buildHeartbeatKey;
import static utils.KeyUtils.buildViewerSetKey;
import static utils.RedisUtils.jedisPool;

/**
* Provides an implementation of HeartbeatService backed by Redis.
*/
public class RedisHeartbeatService implements HeartbeatService
{

    private final int viewerExpirySeconds;
    private final int viewerSetExpirySeconds;

    public RedisHeartbeatService()
    {
        this.viewerExpirySeconds = Play.application().configuration().getInt(VIEWER_EXPIRY_SECONDS, VIEWER_EXPIRY_SECONDS_DEFAULT);
        this.viewerSetExpirySeconds = Play.application().configuration().getInt(VIEWER_SET_EXPIRY_SECONDS, VIEWER_SET_EXPIRY_SECONDS_DEFAULT);
    }

    @Override
    public Map<String, String> list(final String hostId, final String resourceId)
    {

        final String resourceKey = buildViewerSetKey(hostId, resourceId);
        Jedis j = jedisPool().getResource();
        Map<String, String> activeViewers = Maps.newHashMap();
        try
        {
            Set<String> allUserIds  = j.smembers(resourceKey);
            j.expire(resourceKey, viewerSetExpirySeconds);

            // Filter out expired viewers
            for (String userId : allUserIds)
            {
                String heartbeatKey = buildHeartbeatKey(hostId, resourceId, userId);
                String lastSeen = j.get(heartbeatKey);
                if (StringUtils.isBlank(lastSeen))
                {
                    // Viewer has expired, remove them from resource set.
                    j.srem(resourceKey, userId);
                }
                else
                {
                    activeViewers.put(userId, lastSeen);
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
    public void put(final String hostId, final String resourceId, final String userId)
    {
        final String heartbeatKey = buildHeartbeatKey(hostId, resourceId, userId);
        final String resourceKey = buildViewerSetKey(hostId, resourceId);

        Jedis j = jedisPool().getResource();
        try
        {
            Transaction t = j.multi();
            t.sadd(resourceKey, userId);
            t.set(heartbeatKey, String.valueOf(System.currentTimeMillis()));
            t.expire(heartbeatKey, viewerExpirySeconds);
            t.expire(resourceKey, viewerSetExpirySeconds);
            t.exec();
        }
        finally
        {
            jedisPool().returnResource(j);
        }
    }

    @Override
    public void delete(final String hostId, final String resourceId, final String viewer)
    {
        String key = buildHeartbeatKey(hostId, resourceId, viewer);
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

}
