package controllers;

import java.util.Map;
import java.util.Objects;

import com.atlassian.connect.play.java.AC;
import com.atlassian.connect.play.java.token.CheckValidToken;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.hash.Hashing;

import play.Logger;
import play.api.mvc.Headers;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import service.*;

import static java.lang.String.format;

public class Viewers extends Controller
{
    private final static String USER_VALIDATION_HEADER = "x-user-validation-token";

    private final HeartbeatService heartbeatService = new RedisHeartbeatService();
    private final ViewerDetailsService viewerDetailsService = new ViewerDetailsService(heartbeatService);
    private final ViewerValidationService viewerValidationService = new RedisViewerValidationService();
    private final MetricsService metricsService = new MetricsService();

    @CheckValidToken(allowInsecurePolling = true)
    public Result put(final String hostId, final String resourceId, final String userMarker)
    {
        metricsService.incCounter("page-hit.viewers-put");
        Logger.trace(format("Putting %s/%s/%s", hostId, resourceId, userMarker));

        return withValidatedParameters(hostId, resourceId, userMarker, new Function<String, Result>()
        {
            @Override
            public Result apply(String userId)
            {
                heartbeatService.put(hostId, resourceId, userId);

                Map<String, JsonNode> viewersWithDetails = viewerDetailsService.getViewersWithDetails(resourceId, hostId);

                return ok(Json.toJson(viewersWithDetails));
            }
        });
    }

    @CheckValidToken(allowInsecurePolling = true)
    public Result delete(final String hostId, final String resourceId, final String userMarker)
    {
        metricsService.incCounter("page-hit.viewers-delete");
        Logger.trace(format("Deleting %s/%s/%s", hostId, resourceId, userMarker));

        return withValidatedParameters(hostId, resourceId, userMarker, new Function<String, Result>()
        {
            @Override
            public Result apply(String userId)
            {
                heartbeatService.delete(hostId, resourceId, userId);
                return noContent();
            }
        });
    }

    private Result withValidatedParameters(String hostId, String resourceId, String userMarker, Function<String, Result> f)
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
        // This prevents a user from pretending someone else is looking at the issue.
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

        // If the user is requesting viewers of a given issue, we expect the request to carry proof that they have visited
        // the view issue page for that issue.
        // This prevents a user from retrieving viewers of an issue they have never seen (and potentially aren't allowed to see).
        String tokenHeader = request().getHeader(USER_VALIDATION_HEADER);
        if (Objects.isNull(tokenHeader)) {
            return unauthorized("Could not confirm user is able to access resource (no token).");
        }
        if (!viewerValidationService.verifyToken(hostId, resourceId, tokenUser, tokenHeader)) {
            return unauthorized("Could not confirm user is able to access resource (bad token).");
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
