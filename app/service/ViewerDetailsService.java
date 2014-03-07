package service;

import com.atlassian.connect.play.java.AC;
import com.atlassian.connect.play.java.AcHost;
import com.atlassian.fugue.Option;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import com.newrelic.api.agent.Trace;

import org.apache.commons.lang3.StringUtils;

import play.Logger;
import play.Play;
import play.cache.Cache;
import play.libs.F.Callback;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;

import java.util.Map;

import static utils.Constants.DISPLAY_NAME_CACHE_EXPIRY_SECONDS;
import static utils.Constants.DISPLAY_NAME_CACHE_EXPIRY_SECONDS_DEFAULT;
import static utils.KeyUtils.buildDisplayNameKey;

/**
 * Retrieves user details from remote hosts.
 */
public class ViewerDetailsService
{

    public static final String DISPLAY_NAME = "displayName";

    private final HeartbeatService heartbeatService;
    private final int displayNameCacheExpirySeconds;

    public ViewerDetailsService(final HeartbeatService heartbeatService)
    {
        this.heartbeatService = heartbeatService;
        this.displayNameCacheExpirySeconds = Play.application().configuration().getInt(DISPLAY_NAME_CACHE_EXPIRY_SECONDS, DISPLAY_NAME_CACHE_EXPIRY_SECONDS_DEFAULT);
    }

    /**
     * @return map of userids actively viewing <code>resourceId</code> on <code>hostId</code> to any additional details
     *         we have about the user.
     */
    public Map<String, JsonNode> getViewersWithDetails(final String resourceId, final String hostId)
    {
        Map<String, String> viewers = heartbeatService.list(hostId, resourceId);

        Map<String, JsonNode> viewersWithDetails = Maps.transformEntries(viewers, new Maps.EntryTransformer<String, String, JsonNode>() {
            @Override
            public JsonNode transformEntry(String username, String lastSeen) {
                ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
                objectNode.put("displayName", getCachedDisplayNameFor(hostId, username).getOrElse(username));
                objectNode.put("lastSeen", lastSeen);
                return objectNode;
            }
        });
        return viewersWithDetails;
    }

    /**
     * Query cache for user's display name. Return if present in cache. Otherwise, initiate cache population and return
     * null, so the full name is available in the future. Non-blocking.
     *
     * @return user display name, or null if not yet known.
     */
    private Option<String> getCachedDisplayNameFor(final String hostId, final String username)
    {

        final String key = buildDisplayNameKey(hostId, username);
        String cachedValue = (String) Cache.get(key);

        if (StringUtils.isNotEmpty(cachedValue))
        {
            // Found cached value, return it immediately.
            return Option.some(cachedValue);
        }

        Logger.info(String.format("Cache miss. Requesting details for %s on %s...", username, hostId));

        Option<? extends AcHost> acHost = AC.getAcHost(hostId);

        if (acHost.isEmpty())
        {
            Logger.warn("Could not find host for id: " + acHost);
            return Option.none();
        }

        Promise<Response> promise = AC.url("/rest/api/2/user", acHost.get(), Option.some(username)).setQueryParameter("username", username).get();

        promise.onRedeem(new Callback<WS.Response>()
        {
            @Override
            @Trace(metricName="process-viewer-details", dispatcher=true)
            public void invoke(Response a) throws Throwable
            {
                JsonNode userDetailsJson = a.asJson();
                JsonNode displayNameNode = userDetailsJson.get(DISPLAY_NAME);
                if (displayNameNode != null)
                {
                    String displayName = displayNameNode.asText();
                    Logger.info(String.format("Obtained display name for %s on %s: %s", username, hostId, displayName));
                    Cache.set(key, displayName, displayNameCacheExpirySeconds);
                }
                else
                {
                    Logger.error("Could not extract full name from user details, which were: " + userDetailsJson.toString());
                }
            }

        });

        promise.recover(new Function<Throwable, WS.Response>()
        {
            @Override
            @Trace(metricName="process-viewer-details-error", dispatcher=true)
            public WS.Response apply(Throwable t)
            {
                // Can't really recover from this, so just rethrow.
                throw new RuntimeException(t);
            }
        });

        return Option.none();
    }

}
