package controllers;

import java.util.Map;

import com.atlassian.connect.play.java.AC;

import com.fasterxml.jackson.databind.JsonNode;

import org.apache.commons.lang3.StringUtils;

import oauth.jwt.compat.CheckValidRequest;
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

    @CheckValidRequest
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
        final Map<String, JsonNode> viewersWithDetails = viewerDetailsService.getViewersWithDetails(resourceId, hostId);

        return ok(views.html.poller.render(Json.toJson(viewersWithDetails).toString(), hostId, resourceId, userId));

    }

}