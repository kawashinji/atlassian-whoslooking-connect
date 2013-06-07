package controllers;

import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jackson.JsonNode;

import play.api.libs.Crypto;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import service.ViewerDetailsService;

import com.atlassian.connect.play.java.AC;
import com.atlassian.connect.play.java.CheckValidOAuthRequest;
import com.atlassian.whoslooking.model.Viewables;

public class Poller extends Controller
{

    @CheckValidOAuthRequest
    public static Result index() throws Exception
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

        Viewables.putViewer(hostId, resourceId, username);

        // Prime cache with user details for any views that don't yet have their details cached.
        // We can currently only do this from within an @CheckValidOAuthRequest'd request.
        // This is non-blocking.
        Map<String, JsonNode> viewersWithDetails = Viewables.getViewersWithDetails(resourceId, hostId);
        for (Entry<String, JsonNode> entry : viewersWithDetails.entrySet())
        {
            if (entry.getValue() == null)
            {
                ViewerDetailsService.primeCacheFor(hostId, entry.getKey());
            }
        }

        // Render poller
        return ok(views.html.poller.render(Json.toJson(viewersWithDetails).toString(), resourceId));
    }

}