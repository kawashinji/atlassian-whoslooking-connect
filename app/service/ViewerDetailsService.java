package service;

import org.codehaus.jackson.JsonNode;

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
     * null.
     * 
     * @return user details JsonNode, or null if not yet known.
     */
    public static JsonNode primeCacheFor(final String hostId, final String username)
    {

        Object value = Cache.get(hostId + "-" + username + "-details");

        if (value == null || !(value instanceof JsonNode))
        {
            if (AC.getUser() != null)
            {

                Logger.debug(String.format("Cache miss. Requesting details for %s on %s...", username, hostId));
                Promise<Response> promise = AC.url("/rest/api/latest/user").setQueryParameter("username", username).get();

                promise.onRedeem(new Callback<WS.Response>()
                {
                    @Override
                    public void invoke(Response a) throws Throwable
                    {
                        JsonNode asJson = a.asJson();

                        if (!asJson.has("errorMessages"))
                        {
                            Cache.set(hostId + "-" + username + "-details",
                                      asJson,
                                      Play.application().configuration()
                                          .getInt("whoslooking.user-details-cache-expiry.seconds", 600));
                        }
                        else
                        {
                            Logger.error(asJson.toString());
                        }
                    }
                });

                promise.recover(new Function<Throwable, WS.Response>()
                {
                    @Override
                    public WS.Response apply(Throwable t)
                    {
                        Logger.error("An error occurred", t);
                        // Can't really recover from this, so just rethrow.
                        throw new RuntimeException(t);
                    }
                });
            }

            return null;
        }
        else
        {
            return (JsonNode) value;
        }

    }

    /**
     * @return user details if cached, null otherwise.
     */
    public static JsonNode getCachedDetailsFor(final String hostId, final String username)
    {
        Object value = Cache.get(hostId + "-" + username + "-details");
        return (JsonNode) value;
    }

}
