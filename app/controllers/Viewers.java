package controllers;

import java.util.Map;

import org.codehaus.jackson.JsonNode;

import play.mvc.Http.Cookie;

import play.Logger;
import play.libs.Crypto;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import service.RedisHeartbeatService;
import service.HeartbeatService;
import service.ViewerDetailsService;

import static utils.Constants.PER_PAGE_VIEW_TOKEN_HEADER;
import static java.lang.String.format;

public class Viewers extends Controller
{
    private final HeartbeatService heartbeatService = new RedisHeartbeatService();
    private final ViewerDetailsService viewerDetailsService = new ViewerDetailsService(heartbeatService);

    public Result put(final String hostId, final String resourceId, final String userId)
    {
        Logger.debug(format("Putting %s/%s/%s", hostId, resourceId, userId));

        if (!isValidRequestFromAuthenticatedUser(hostId, userId))
        {
            return badRequest("Don't spoof me bro.");
        }

        heartbeatService.put(hostId, resourceId, userId);

        Map<String, JsonNode> viewersWithDetails = viewerDetailsService.getViewersWithDetails(resourceId, hostId);

        return ok(Json.toJson(viewersWithDetails));
    }

    public Result delete(final String hostId, final String resourceId, final String userId)
    {
        Logger.debug(format("Deleting %s/%s/%s", hostId, resourceId, userId));

        if (!isValidRequestFromAuthenticatedUser(hostId, userId))
        {
            return badRequest("Don't spoof me bro.");
        }

        heartbeatService.delete(hostId, resourceId, userId);

        return noContent();
    }

    private boolean isValidRequestFromAuthenticatedUser(final String hostId, final String username)
    {
        final String token = request().getHeader(PER_PAGE_VIEW_TOKEN_HEADER);
        final String expectedToken = Crypto.sign(hostId + username);
        Logger.trace(format("Token check for %s on %s: received=%s expected=%s", username, hostId, token, expectedToken));

        return expectedToken.equals(token) || isValidLegacyRequest(hostId, expectedToken);
    }

    /**
     * This is legacy code to be removed shortly. Avoids throwing errors on receiving request from tabs that have been open for multiple upgrades.
     *
     * @return true if client is sending a valid token in the cookie.
     */
    private boolean isValidLegacyRequest(final String hostId, final String expectedToken)
    {
        Cookie signedIdCookie = request().cookie("signed-identity-on-" + hostId);
        boolean validLegacyRequest = false;
        if (signedIdCookie != null)
        {
            Logger.info("Cookie found, looks like a legacy request.");
            validLegacyRequest = expectedToken.equals(signedIdCookie.value());
        }
        return validLegacyRequest;
    }

}
