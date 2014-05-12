package controllers;

import java.util.Map;

import com.atlassian.connect.play.java.AC;
import com.atlassian.connect.play.java.auth.jwt.AuthenticateJwtRequest;

import com.fasterxml.jackson.databind.JsonNode;

import org.apache.commons.lang3.StringUtils;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import service.AnalyticsService;
import service.HeartbeatService;
import service.RedisAnalyticsService;
import service.RedisHeartbeatService;
import service.ViewerDetailsService;

import static play.api.libs.Codecs.sha1;
import static service.AnalyticsService.ACTIVE_HOST_V2;
import static service.AnalyticsService.ACTIVE_USER_V2;

public class Poller extends Controller
{
    private final HeartbeatService heartbeatService = new RedisHeartbeatService();
    private final ViewerDetailsService viewerDetailsService = new ViewerDetailsService(heartbeatService);
    private final AnalyticsService analyticsService = new RedisAnalyticsService();

    @AuthenticateJwtRequest
    public Result index() throws Exception
    {
        final String hostId = AC.getAcHost().getKey();
        final String resourceId = request().getQueryString("issue_key");
        final String userId = AC.getUser().getOrNull();

        if (StringUtils.isBlank(userId))
        {
            return unauthorized(views.html.anonymous.render(hostId, resourceId, userId));
        }
        
        heartbeatService.put(hostId, resourceId, userId);
        
        analyticsService.fire(ACTIVE_HOST_V2, sha1(hostId));
        analyticsService.fire(ACTIVE_USER_V2, sha1(hostId)+":"+sha1(userId));
        
        final Map<String, JsonNode> viewersWithDetails = viewerDetailsService.getViewersWithDetails(resourceId, hostId);
        return ok(views.html.poller.render(Json.toJson(viewersWithDetails).toString(), hostId, resourceId, userId));

    }

}