package controllers;

import java.util.Map;

import com.atlassian.connect.play.java.AC;
import com.atlassian.connect.play.java.token.CheckValidToken;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.hash.Hashing;

import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import service.AnalyticsService;
import service.HeartbeatService;
import service.RedisAnalyticsService;
import service.RedisHeartbeatService;
import service.ViewerDetailsService;

import static java.lang.String.format;

public class Viewers extends Controller
{
    private final HeartbeatService heartbeatService = new RedisHeartbeatService();
    private final AnalyticsService analyticsService = new RedisAnalyticsService();
    private final ViewerDetailsService viewerDetailsService = new ViewerDetailsService(heartbeatService);

    @CheckValidToken(allowInsecurePolling = true)
    public Result put(final String hostId, final String resourceId, final String userMarker)
    {
        Logger.trace(format("Putting %s/%s/%s", hostId, resourceId, userMarker));

        return withValidatedParameters(hostId, userMarker, new Function<String, Result>()
        {
            @Override
            public Result apply(String userId)
            {
                heartbeatService.put(hostId, resourceId, userId);

                Map<String, JsonNode> viewersWithDetails = viewerDetailsService.getViewersWithDetails(resourceId,
                                                                                                      hostId);
                analyticsService.fire(AnalyticsService.ACTIVE_USER, hostId + userId);
                analyticsService.fire(AnalyticsService.ACTIVE_HOST, hostId);

                return ok(Json.toJson(viewersWithDetails));
            }
        });
    }

    @CheckValidToken(allowInsecurePolling = true)
    public Result delete(final String hostId, final String resourceId, final String userMarker)
    {
        Logger.trace(format("Deleting %s/%s/%s", hostId, resourceId, userMarker));

        return withValidatedParameters(hostId, userMarker, new Function<String, Result>()
        {
            @Override
            public Result apply(String userId)
            {
                heartbeatService.delete(hostId, resourceId, userId);
                return noContent();
            }
        });
    }

    private Result withValidatedParameters(String hostId, String userMarker, Function<String, Result> f)
    {
        if (!hostId.equals(AC.getAcHost().getKey()))
        {
            return unauthorized("Token host ["
                                + AC.getAcHost().getKey()
                                + "] does not match URL host ["
                                + hostId
                                + "].");
        }

        // If the user is specified in the URL, we expect it to be the exact username from the token, or its sha1 hash.
        String tokenUser = AC.getUser().get();
        String tokenUserSha1 = Hashing.sha1().hashString(tokenUser, Charsets.UTF_8).toString();
        if (!userMarker.equals(tokenUser) && !userMarker.equals(tokenUserSha1))
        {
            return unauthorized("Neither token user ["
                                + tokenUser
                                + "] nor its sha1 hash ["
                                + tokenUserSha1
                                + "] match URL user ["
                                + userMarker
                                + "].");
        }

        return AC.getUser().fold(new Supplier<Result>()
        {
            @Override
            public Result get()
            {
                return unauthorized("Could not validate user. Invalid token?");
            }
        }, f);
    }

}
