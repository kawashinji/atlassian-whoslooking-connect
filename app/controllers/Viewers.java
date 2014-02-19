package controllers;


import java.util.Map;

import com.atlassian.connect.play.java.token.CheckValidToken;

import com.fasterxml.jackson.databind.JsonNode;

import service.RedisAnalyticsService;

import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import service.AnalyticsService;
import service.HeartbeatService;
import service.RedisHeartbeatService;
import service.ViewerDetailsService;
import static java.lang.String.format;

public class Viewers extends Controller
{
    private final HeartbeatService heartbeatService = new RedisHeartbeatService();
    private final AnalyticsService analyticsService = new RedisAnalyticsService();
    private final ViewerDetailsService viewerDetailsService = new ViewerDetailsService(heartbeatService);

    @CheckValidToken(allowInsecurePolling = true)
    public Result put(final String hostId, final String resourceId, final String userId)
    {
        Logger.debug(format("Putting %s/%s/%s", hostId, resourceId, userId));

        heartbeatService.put(hostId, resourceId, userId);

        Map<String, JsonNode> viewersWithDetails = viewerDetailsService.getViewersWithDetails(resourceId, hostId);
        analyticsService.fire(AnalyticsService.ACTIVE_USER, hostId+userId);
        analyticsService.fire(AnalyticsService.ACTIVE_HOST, hostId);

        return ok(Json.toJson(viewersWithDetails));
    }

    @CheckValidToken(allowInsecurePolling = true)
    public Result delete(final String hostId, final String resourceId, final String userId)
    {
        Logger.debug(format("Deleting %s/%s/%s", hostId, resourceId, userId));

        heartbeatService.delete(hostId, resourceId, userId);

        return noContent();
    }

}
