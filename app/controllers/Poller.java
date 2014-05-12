package controllers;

import java.util.Map;

import com.atlassian.connect.play.java.AC;
import com.atlassian.connect.play.java.auth.jwt.AuthenticateJwtRequest;

import com.fasterxml.jackson.databind.JsonNode;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.security.Credential.MD5;

import static org.eclipse.jetty.util.security.Credential.MD5.digest;

import static service.AnalyticsService.ACTIVE_HOST_V2;
import static service.AnalyticsService.ACTIVE_USER_V2;
import service.AnalyticsService;
import service.RedisAnalyticsService;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import service.HeartbeatService;
import service.RedisHeartbeatService;
import service.ViewerDetailsService;

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
        
        analyticsService.fire(ACTIVE_HOST_V2, digest(hostId));
        analyticsService.fire(ACTIVE_USER_V2, digest(hostId)+":"+digest(userId));
        
        final Map<String, JsonNode> viewersWithDetails = viewerDetailsService.getViewersWithDetails(resourceId, hostId);
        return ok(views.html.poller.render(Json.toJson(viewersWithDetails).toString(), hostId, resourceId, userId));

    }

}