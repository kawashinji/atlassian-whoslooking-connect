package service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;

import play.Play;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.Tuple;

import static utils.Constants.VIEWER_EXPIRY_SECONDS;
import static utils.Constants.VIEWER_EXPIRY_SECONDS_DEFAULT;
import static utils.Constants.VIEWER_SET_EXPIRY_SECONDS;
import static utils.Constants.VIEWER_SET_EXPIRY_SECONDS_DEFAULT;
import static utils.KeyUtils.buildHeartbeatKey;
import static utils.KeyUtils.buildViewerSetKey;
import static utils.KeyUtils.extractUseridFromHeartbeatKey;
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
        final long nowMs = System.currentTimeMillis();
        
        Jedis j = jedisPool().getResource();
        Map<String, String> activeViewers = Maps.newHashMap();
        try
        {
            j.expire(resourceKey, viewerSetExpirySeconds);
            Set<Tuple> activeViewersSet = j.zrangeByScoreWithScores(resourceKey, nowMs - TimeUnit.SECONDS.toMillis(viewerExpirySeconds), Double.MAX_VALUE);
            
            for (Tuple t : activeViewersSet)
            {
                activeViewers.put(extractUseridFromHeartbeatKey(t.getElement()), String.valueOf(t.getScore()));
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
        final long nowMs = System.currentTimeMillis();

        Jedis j = jedisPool().getResource();
        try
        {
            Transaction t = j.multi();
            t.zremrangeByScore(resourceKey, 0, nowMs - TimeUnit.SECONDS.toMillis(viewerExpirySeconds));
            t.zadd(resourceKey, nowMs, heartbeatKey);
            t.expire(resourceKey, viewerSetExpirySeconds);
            t.exec();
        }
        finally
        {
            jedisPool().returnResource(j);
        }
    }

    @Override
    public void delete(final String hostId, final String resourceId, final String userId)
    {
        final String heartbeatKey = buildHeartbeatKey(hostId, resourceId, userId);
        final String resourceKey = buildViewerSetKey(hostId, resourceId);
        
        Jedis j = jedisPool().getResource();
        try
        {
            j.zrem(resourceKey, heartbeatKey);
        }
        finally
        {
            jedisPool().returnResource(j);
        }
    }
    
    public long activeUsers(int days)
    {
        final long nowMs = System.currentTimeMillis();
        
        Jedis j = jedisPool().getResource();
        try
        {
            return j.zcount("active-users", nowMs - TimeUnit.DAYS.toMillis(days), Double.MAX_VALUE);
        }
        finally
        {
            jedisPool().returnResource(j);
        }
    }
    
    public long activeHosts(int days)
    {
        final long nowMs = System.currentTimeMillis();
        
        Jedis j = jedisPool().getResource();
        try
        {
            return j.zcount("active-hosts", nowMs - TimeUnit.DAYS.toMillis(days), Double.MAX_VALUE);
        }
        finally
        {
            jedisPool().returnResource(j);
        }
    }
    
    

}
