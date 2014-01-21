package controllers;

import com.atlassian.connect.play.java.AC;
import com.atlassian.connect.play.java.auth.jwt.AuthenticateJwtRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import play.api.libs.Crypto;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import service.HeartbeatService;
import service.RedisHeartbeatService;
import service.ViewerDetailsService;

import java.util.Map;

public class Poller extends Controller
{
    private final HeartbeatService heartbeatService = new RedisHeartbeatService();
    private final ViewerDetailsService viewerDetailsService = new ViewerDetailsService(heartbeatService);

    @AuthenticateJwtRequest
    public Result index() throws Exception
    {
        final String hostId = request().queryString().get("oauth_consumer_key")[0];
        final String resourceId = request().getQueryString("issue_key");
        final String userId = AC.getUser().getOrNull();

        if (StringUtils.isBlank(userId))
        {
            return unauthorized(views.html.anonymous.render(hostId, resourceId, userId));
        }

        heartbeatService.put(hostId, resourceId, userId);
        final Map<String, JsonNode> viewersWithDetails = viewerDetailsService.getViewersWithDetails(resourceId, hostId);

        final String perPageViewToken = Crypto.sign(hostId + userId);
        return ok(views.html.poller.render(Json.toJson(viewersWithDetails).toString(), hostId, resourceId, userId, perPageViewToken));

    }

}