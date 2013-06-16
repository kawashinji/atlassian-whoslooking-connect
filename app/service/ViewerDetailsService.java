package service;

import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;

import play.libs.Json;

import play.Logger;
import play.Play;
import play.cache.Cache;
import play.libs.F.Callback;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;

import com.atlassian.connect.play.java.AC;

public class ViewerDetailsService
{

    /**
     * Query cache for user details. Return details if present in cache. Otherwise, initiate cache population and return
     * null, so the details are available in the future. Non-blocking.
     *
     * @return user details JSON String, or null if not yet known.
     */
    public static JsonNode getCachedDetailsFor(final String hostId, final String username)
    {

        String cachedValue = safeCacheGet(hostId, username);

        if (StringUtils.isNotEmpty(cachedValue))
        {
            // Found cached value, return it immediately.
            return Json.parse(cachedValue);
        }

        Logger.info(String.format("Cache miss. Requesting details for %s on %s...", username, hostId));

        if (AC.getUser() == null || AC.getAcHost() == null)
        {
            Logger.warn("Cannot request user details from host without an authenticated user context.");
            return null;
        }

        Promise<Response> promise = AC.url("/rest/api/latest/user").setQueryParameter("username", username).get();

        promise.onRedeem(new Callback<WS.Response>()
        {
            @Override
            public void invoke(Response a) throws Throwable
            {
                JsonNode userDetailsJson = a.asJson();
                if (!userDetailsJson.has("errorMessages"))
                {
                    Logger.info(String.format("Obtained details for %s on %s.", username, hostId));
                    Cache.set(hostId + "-" + username + "-details", userDetailsJson.toString(), getCacheExpiry());
                }
                else
                {
                    Logger.error(userDetailsJson.toString());
                }
            }

        });

        promise.recover(new Function<Throwable, WS.Response>()
        {
            @Override
            public WS.Response apply(Throwable t)
            {
                // Can't really recover from this, so just rethrow.
                throw new RuntimeException(t);
            }
        });

        return null;
    }

    private static String safeCacheGet(final String hostId, final String username)
    {
        String cachedValue;
        try
        {
            cachedValue = (String) Cache.get(hostId + "-" + username + "-details");
        }
        catch (Exception e)
        {
            // Redis cache NPE's when the value doesn't exist... :(
            cachedValue = null;
        }
        return cachedValue;
    }

    private static int getCacheExpiry()
    {
        return Play.application().configuration().getInt("whoslooking.user-details-cache-expiry.seconds", 604800);
    }

}
