package service;

import java.util.concurrent.TimeUnit;

import com.atlassian.connect.play.java.AC;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;

import play.Logger;
import play.Play;
import play.cache.Cache;
import play.libs.F.Callback;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;

public class ViewerDetailsService
{

    public static final String DISPLAY_NAME = "displayName";

    public static final int DISPLAY_NAME_EXPIRY_SECONDS = Play.application().configuration()
                                                              .getInt("whoslooking.display-name-cache-expiry.seconds",
                                                                      (int)TimeUnit.DAYS.toSeconds(2));


    /**
     * Query cache for user's display name. Return if present in cache. Otherwise, initiate cache population and return
     * null, so the full name is available in the future. Non-blocking.
     *
     * @return user display name, or null if not yet known.
     */
    public static String getCachedDisplayNameFor(final String hostId, final String username)
    {

        final String key = buildDisplayNameKey(hostId, username);
        String cachedValue = (String) Cache.get(key);

        if (StringUtils.isNotEmpty(cachedValue))
        {
            // Found cached value, return it immediately.
            return cachedValue;
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
                JsonNode displayNameNode = userDetailsJson.get(DISPLAY_NAME);
                if (displayNameNode != null)
                {
                    String displayName = displayNameNode.asText();
                    Logger.info(String.format("Obtained display name for %s on %s: %s", username, hostId, displayName));
                    Cache.set(key, displayName, DISPLAY_NAME_EXPIRY_SECONDS);
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
            public WS.Response apply(Throwable t)
            {
                // Can't really recover from this, so just rethrow.
                throw new RuntimeException(t);
            }
        });

        return null;
    }


    private static String buildDisplayNameKey(final String hostId, final String username)
    {
        return hostId + "-" + username + "-"  + DISPLAY_NAME;
    }


}
