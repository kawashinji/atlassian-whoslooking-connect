package service;

import java.util.Map;
import java.util.Random;

import com.atlassian.connect.play.java.AC;
import com.atlassian.connect.play.java.AcHost;
import com.atlassian.fugue.Option;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Supplier;
import com.google.common.collect.Maps;

import org.javasimon.Split;

import play.Logger;
import play.Play;
import play.cache.Cache;
import play.libs.F.Callback;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;
import redis.clients.jedis.Jedis;
import static com.google.common.collect.Maps.transformEntries;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static utils.Constants.DISPLAY_NAME_CACHE_EXPIRY_SECONDS;
import static utils.Constants.DISPLAY_NAME_CACHE_EXPIRY_SECONDS_DEFAULT;
import static utils.Constants.DISPLAY_NAME_FETCH_BLACKLIST_EXPIRY_SECONDS;
import static utils.Constants.DISPLAY_NAME_FETCH_BLACKLIST_EXPIRY_SECONDS_DEFAULT;
import static utils.Constants.ENABLE_DISPLAY_NAME_FETCH;
import static utils.Constants.ENABLE_DISPLAY_NAME_FETCH_BLACKLIST;
import static utils.KeyUtils.buildDisplayNameKey;
import static utils.RedisUtils.jedisPool;

/**
 * Retrieves user details from remote hosts.
 */
public class ViewerDetailsService
{

    private static final int BLACKLIST_FAILURE_THRESHOLD = 3;

    public static final String DISPLAY_NAME = "displayName";

    private final HeartbeatService heartbeatService;
    private final int displayNameCacheExpirySeconds;
    private final int displayNameFetchBlacklistExpirySeconds;
    private final Random random = new Random();

    private boolean enableDisplayNameFetchBlackList;
    private boolean enableDisplayNameFetch;

    private final MetricsService metricsService = new MetricsService();

    public ViewerDetailsService(final HeartbeatService heartbeatService)
    {
        this.heartbeatService = heartbeatService;
        this.displayNameCacheExpirySeconds = Play.application().configuration().getInt(DISPLAY_NAME_CACHE_EXPIRY_SECONDS, DISPLAY_NAME_CACHE_EXPIRY_SECONDS_DEFAULT);
        this.enableDisplayNameFetch = Play.application().configuration().getBoolean(ENABLE_DISPLAY_NAME_FETCH, true);
        this.enableDisplayNameFetchBlackList = Play.application().configuration().getBoolean(ENABLE_DISPLAY_NAME_FETCH_BLACKLIST, true);
        this.displayNameFetchBlacklistExpirySeconds = Play.application().configuration().getInt(DISPLAY_NAME_FETCH_BLACKLIST_EXPIRY_SECONDS, DISPLAY_NAME_FETCH_BLACKLIST_EXPIRY_SECONDS_DEFAULT);
    }

    /**
     * @return map of userids actively viewing <code>resourceId</code> on <code>hostId</code> to any additional details
     *         we have about the user.
     */
    public Map<String, JsonNode> getViewersWithDetails(final String resourceId, final String hostId)
    {
        return metricsService.withMetric("viewer-details", new Supplier<Map<String, JsonNode>>() {
            @Override
            public Map<String, JsonNode> get()
            {
                Map<String, String> viewers = heartbeatService.list(hostId, resourceId);
                Map<String, JsonNode> viewersWithDetails = transformEntries(viewers, new Maps.EntryTransformer<String, String, JsonNode>() {
                    @Override
                    public JsonNode transformEntry(String accountId, String lastSeen) {
                        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
                        objectNode.put("displayName", getCachedDisplayNameFor(hostId, accountId).getOrElse(accountId));
                        return objectNode;
                    }
                });
                
                return viewersWithDetails;
            }
        });
    }

    private void asyncFetch(final String hostId, final String accountId, final String key)
    {
        Logger.info(String.format("Cache miss. Requesting details for %s on %s...", accountId, hostId));

        Option<? extends AcHost> acHost = AC.getAcHost(hostId);

        if (acHost.isEmpty())
        {
            Logger.warn("Could not find host for id: " + acHost);
            return;
        }

        Promise<Response> promise = AC.url("/rest/api/3/user", acHost.get(), Option.<String> none()).setQueryParameter("accountId", accountId)
            .setTimeout(5000)
            .get();
        
        final Option<Split> timer = metricsService.start("displayname.request");
        
        promise.onRedeem(new Callback<WS.Response>()
        {
            @Override
            public void invoke(Response a) throws Throwable
            {
                metricsService.stop(timer);
                JsonNode userDetailsJson = a.asJson();
                JsonNode displayNameNode = userDetailsJson.get(DISPLAY_NAME);
                if (displayNameNode != null)
                {
                    String displayName = displayNameNode.asText();
                    Logger.info(String.format("Obtained display name for accountId %s on %s: %s", accountId, hostId, displayName));
                    int jitter = random.nextInt(displayNameCacheExpirySeconds);
                    Cache.set(key, displayName, displayNameCacheExpirySeconds + jitter);
                }
                else
                {
                    Logger.error("Could not extract display name from accountId details, which were: " + userDetailsJson.toString());
                    recordFailure(key);
                }
            }
        });
        
        promise.onFailure(new Callback<Throwable>() {
            @Override
            public void invoke(Throwable t) throws Throwable
            {
                metricsService.stop(timer);
                Logger.error("Could not obtain display name for user " + key + ": ", t);
                recordFailure(key);
            }
        });
    }
    
    /**
     * Query cache for user's display name. Return if present in cache. Otherwise, initiate cache population and return
     * null, so the display name is available in the future. Non-blocking.
     * 
     * @return user display name, or null if not yet known.
     */
    private Option<String> getCachedDisplayNameFor(final String hostId, final String accountId)
    {

        final String key = buildDisplayNameKey(hostId, accountId);
        String cachedValue = (String) Cache.get(key);

        if (isNotEmpty(cachedValue))
        {
            // Found cached value, return it immediately.
            metricsService.incCounter("displayname.cache-hits");
            return Option.some(cachedValue);
        }
        else
        {
            metricsService.incCounter("displayname.cache-misses");

            if (isBlackListed(key) || !enableDisplayNameFetch)
            {
                metricsService.incCounter("displayname.blocked-requests");
            }
            else
            {
                // Populate the cache in the background and return none for now
                asyncFetch(hostId, accountId, key);
            }
        }

        return Option.none();
    }

    
    /**
     * Should the attempt to retrieve the user's display name be prevented? This might happen if the feature is disabled, or too many failures to retrieve the user's display name have occured in recent history.
     * 
     * @return false if the display name retrieval should be attempted, true if not.
     */
    private boolean isBlackListed(String key)
    {
        if (!enableDisplayNameFetchBlackList)
        {
            return false;
        }
        Jedis j = jedisPool().getResource();
        try
        {
            String errorCount = j.get("displayname-blacklist-"+key);
            return errorCount != null && Integer.valueOf(errorCount) > BLACKLIST_FAILURE_THRESHOLD;
        }
        finally
        {
            jedisPool().returnResource(j);
        }
    }
    
    /**
     * Record failure to retrieve display name, so repeated failures are blacklisted. 
     * 
     * @param key
     */
    private void recordFailure(final String key)
    {
        metricsService.incCounter("displayname.fetch-failures");
        if (enableDisplayNameFetchBlackList)
        {
            Jedis j = jedisPool().getResource();
            try
            {
                long failedAttempts = j.incr("displayname-blacklist-"+key);
                j.expire("displayname-blacklist-"+key, displayNameFetchBlacklistExpirySeconds);
                
                if (failedAttempts>BLACKLIST_FAILURE_THRESHOLD)
                {
                    Logger.info("Blacklisting display name retrival for " + key);
                    metricsService.incCounter("displayname.blacklisted-users");
                }              
            }
            finally
            {
                jedisPool().returnResource(j);    
            }
        }
    }

}
