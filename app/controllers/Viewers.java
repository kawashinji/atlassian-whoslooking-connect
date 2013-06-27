package controllers;

import static controllers.Poller.PER_PAGE_VIEW_TOKEN_HEADER;
import static java.lang.String.format;

import java.util.Map;

import org.codehaus.jackson.JsonNode;

import play.Logger;
import play.libs.Crypto;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import service.RedisViewablesService;
import service.ViewablesService;

import com.atlassian.connect.play.java.CheckValidOAuthRequest;

public class Viewers extends Controller
{
    private final ViewablesService viewables = new RedisViewablesService();

    public Result put(final String hostId, final String resourceId, final String userId)
    {
        Logger.debug(format("Putting %s/%s/%s", hostId, resourceId, userId));

        if (!isValidRequestFromAuthenticatedUser(hostId, userId))
        {
            return badRequest("Don't spoof me bro.");
        }

        viewables.putViewer(hostId, resourceId, userId);

        Map<String, JsonNode> viewersWithDetails = viewables.getViewersWithDetails(resourceId, hostId);

        return ok(Json.toJson(viewersWithDetails));
    }

    public Result delete(final String hostId, final String resourceId, final String userId)
    {
        Logger.debug(format("Deleting %s/%s/%s", hostId, resourceId, userId));

        if (!isValidRequestFromAuthenticatedUser(hostId, userId))
        {
            return badRequest("Don't spoof me bro.");
        }

        viewables.deleteViewer(hostId, resourceId, userId);

        return noContent();
    }

    public Result get(final String hostId, final String resourceId, final String userId)
    {
        Logger.debug(format("Getting %s/%s/%s", hostId, resourceId, userId));

        if (!isValidRequestFromAuthenticatedUser(hostId, userId))
        {
            return badRequest("Don't spoof me bro.");
        }

        Map<String, JsonNode> viewersWithDetails = viewables.getViewersWithDetails(resourceId, hostId);

        return ok(Json.toJson(viewersWithDetails.get(userId)));
    }

    @CheckValidOAuthRequest
    public Result list(final String hostId, final String resourceId)
    {
        return ok(Json.toJson(viewables.getViewersWithDetails(resourceId, hostId)));
    }

    private boolean isValidRequestFromAuthenticatedUser(final String hostId, final String username)
    {
        final String token = request().getHeader(PER_PAGE_VIEW_TOKEN_HEADER);
        final String expectedToken = Crypto.sign(hostId + username);
        Logger.trace(format("Token check for %s on %s: received=%s expected=%s", username, hostId, token, expectedToken));
        
        return expectedToken.equals(token);
    }

}
