package controllers;

import java.util.Map;

import com.atlassian.connect.play.java.CheckValidOAuthRequest;
import com.atlassian.whoslooking.model.Viewables;

import org.codehaus.jackson.JsonNode;

import play.Logger;
import play.libs.Crypto;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

public class Viewers extends Controller
{

    public static Result put(final String hostId_brokenOnUnicorn, final String resourceId, final String userId)
    {
        // Path parsing for hostId failing on Unicorn, why? Have to pass it as a parameter for now.
        final String hostId = (hostId_brokenOnUnicorn != null) ? hostId_brokenOnUnicorn : request().getQueryString("hostId_for_unicorn");

        Logger.debug(String.format("Putting for host %s, resource %s", hostId, resourceId));

        if (!isValidRequestFromAuthenticatedUser(hostId, userId))
        {
            return badRequest("Don't spoof me bro.");
        }

        Viewables.putViewer(hostId, resourceId, userId);

        Map<String, JsonNode> viewersWithDetails = Viewables.getViewersWithDetails(resourceId, hostId);

        return ok(Json.toJson(viewersWithDetails));
    }

    public static Result delete(final String hostId_brokenOnUnicorn, final String resourceId, final String userId)
    {
        // Path parsing for hostId failing on Unicorn, why? Have to pass it as a parameter for now.
        final String hostId = (hostId_brokenOnUnicorn != null) ? hostId_brokenOnUnicorn : request().getQueryString("hostId_for_unicorn");

        Logger.debug(String.format("Deleting for host %s, resource %s, user %s", hostId, resourceId, userId));

        if (!isValidRequestFromAuthenticatedUser(hostId, userId))
        {
            return badRequest("Don't spoof me bro.");
        }

        Viewables.deleteViewer(hostId, resourceId, userId);

        return noContent();
    }


    private static boolean isValidRequestFromAuthenticatedUser(String hostId, String username)
    {
        // This should work, but doesn't. So we've rolled our own.
        // return username.equals(session().get("identity-on-"+hostId))

        Logger.debug(String.format("Cookie key: " + "signed-identity-on-" + hostId));
        Logger.debug(String.format("Cookies: %s", request().cookies()));
        Logger.debug(String.format("Cookie: %s", request().cookies().get("signed-identity-on-" + hostId)));
        String signature = request().cookie("signed-identity-on-" + hostId).value();
        String expectedSignature = Crypto.sign(hostId + username);
        return expectedSignature.equals(signature);
    }

    @CheckValidOAuthRequest
    public static Result get(String hostId, String resourceId)
    {
        Logger.debug(String.format("Returning list of viewers for '%s' on '%s'", resourceId, hostId));
        return ok(Json.toJson(Viewables.getViewersWithDetails(hostId, resourceId)));
    }

    @CheckValidOAuthRequest
    public static Result list()
    {
        return ok(Json.toJson(Viewables.getAll()));
    }

}