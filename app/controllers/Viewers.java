package controllers;

import java.util.Map;

import com.atlassian.connect.play.java.CheckValidOAuthRequest;

import org.codehaus.jackson.JsonNode;

import play.Logger;
import play.libs.Crypto;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import service.RedisViewablesService;
import service.ViewablesService;

public class Viewers extends Controller
{
    private final ViewablesService viewables = new RedisViewablesService();

    public Result put(final String hostId_brokenOnUnicorn, final String resourceId, final String userId)
    {
        // Path parsing for hostId failing on Unicorn, why? Have to pass it as a parameter for now.
        Logger.trace("hostId from path: " + hostId_brokenOnUnicorn);
        final String hostId = (hostId_brokenOnUnicorn != null) ? hostId_brokenOnUnicorn : request().getQueryString("hostId_for_unicorn");

        Logger.debug(String.format("Putting %s/%s/%s", hostId, resourceId, userId));

        if (!isValidRequestFromAuthenticatedUser(hostId, userId))
        {
            return badRequest("Don't spoof me bro.");
        }

        viewables.putViewer(hostId, resourceId, userId);

        Map<String, JsonNode> viewersWithDetails = viewables.getViewersWithDetails(resourceId, hostId);

        return ok(Json.toJson(viewersWithDetails));
    }

    public Result delete(final String hostId_brokenOnUnicorn, final String resourceId, final String userId)
    {
        // Path parsing for hostId failing on Unicorn, why? Have to pass it as a parameter for now.
        Logger.trace("hostId from path: " + hostId_brokenOnUnicorn);
        final String hostId = (hostId_brokenOnUnicorn != null) ? hostId_brokenOnUnicorn : request().getQueryString("hostId_for_unicorn");

        Logger.debug(String.format("Deleting %s/%s/%s", hostId, resourceId, userId));

        if (!isValidRequestFromAuthenticatedUser(hostId, userId))
        {
            return badRequest("Don't spoof me bro.");
        }

        viewables.deleteViewer(hostId, resourceId, userId);

        return noContent();
    }

    public Result get(final String hostId_brokenOnUnicorn, final String resourceId, final String userId)
    {
        // Path parsing for hostId failing on Unicorn, why? Have to pass it as a parameter for now.
        Logger.trace("hostId from path: " + hostId_brokenOnUnicorn);
        final String hostId = (hostId_brokenOnUnicorn != null) ? hostId_brokenOnUnicorn : request().getQueryString("hostId_for_unicorn");

        Logger.debug(String.format("Getting %s/%s/%s", hostId, resourceId, userId));

        if (!isValidRequestFromAuthenticatedUser(hostId, userId))
        {
            return badRequest("Don't spoof me bro.");
        }

        Map<String, JsonNode> viewersWithDetails = viewables.getViewersWithDetails(resourceId, hostId);

        return ok(Json.toJson(viewersWithDetails.get(userId)));
    }

    @CheckValidOAuthRequest
    public Result list(final String hostId_brokenOnUnicorn, final String resourceId)
    {
        // Path parsing for hostId failing on Unicorn, why? Have to pass it as a parameter for now.
        final String hostId = (hostId_brokenOnUnicorn != null) ? hostId_brokenOnUnicorn : request().getQueryString("hostId_for_unicorn");

        return ok(Json.toJson(viewables.getViewersWithDetails(resourceId, hostId)));
    }

    private boolean isValidRequestFromAuthenticatedUser(String hostId, String username)
    {
        // This should work, but doesn't. So we've rolled our own.
        // return username.equals(session().get("identity-on-"+hostId))
        Logger.trace("ID in session: " + session().get("identity-on-"+hostId));

        String signature = request().cookie("signed-identity-on-" + hostId).value();
        String expectedSignature = Crypto.sign(hostId + username);
        return expectedSignature.equals(signature);
    }

}