package controllers;

import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jackson.JsonNode;

import service.RedisViewablesService;

import service.ViewablesService;

import service.ExpiringSetViewablesService;

import play.api.libs.Crypto;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import service.ViewerDetailsService;

import com.atlassian.connect.play.java.AC;
import com.atlassian.connect.play.java.CheckValidOAuthRequest;

public class Poller extends Controller
{

    //private final ViewablesService viewables = new ExpiringSetViewablesService();
    private final ViewablesService viewables = new RedisViewablesService();

    @CheckValidOAuthRequest
    public Result index() throws Exception
    {
        final String hostId = request().queryString().get("oauth_consumer_key")[0];
        final String username = AC.getUser().getOrNull();
        String resourceId = request().getQueryString("issue_id");

        // We have validated that this request is valid for this host+username.
        // Setup session so that subsequent requests from this client are also
        // treated as such, even if they don't include the OAuth signature (i.e.
        // Ajax requests from within the iframe).
        // TODO: this session() call doesn't work (ends up empty on poll calls).. so we have to roll our own.
        session("identity-on-" + hostId, username);
        response().setCookie("identity-on-" + hostId, username);
        response().setCookie("signed-identity-on-" + hostId, Crypto.sign(hostId + username));

        viewables.putViewer(hostId, resourceId, username);

        Map<String, JsonNode> viewersWithDetails = viewables.getViewersWithDetails(resourceId, hostId);

        // Render poller
        return ok(views.html.poller.render(Json.toJson(viewersWithDetails).toString(), resourceId));
    }

}