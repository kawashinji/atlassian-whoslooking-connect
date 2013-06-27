package controllers;

import java.util.Map;

import com.atlassian.connect.play.java.AC;
import com.atlassian.connect.play.java.CheckValidOAuthRequest;

import org.codehaus.jackson.JsonNode;

import play.api.libs.Crypto;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import service.RedisViewablesService;
import service.ViewablesService;

public class Poller extends Controller
{
    public static final String PER_PAGE_VIEW_TOKEN_HEADER = "x-per-page-view-token";
    
    private final ViewablesService viewables = new RedisViewablesService();

    @CheckValidOAuthRequest
    public Result index() throws Exception
    {
        final String hostId = request().queryString().get("oauth_consumer_key")[0];
        final String resourceId = request().getQueryString("issue_id");
        final String userId = AC.getUser().getOrNull();        
        final String perPageViewToken = Crypto.sign(hostId + userId);
        
        viewables.putViewer(hostId, resourceId, userId);
        final Map<String, JsonNode> viewersWithDetails = viewables.getViewersWithDetails(resourceId, hostId);
        return ok(views.html.poller.render(Json.toJson(viewersWithDetails).toString(),
                                           hostId, resourceId, userId, perPageViewToken));
    }

}