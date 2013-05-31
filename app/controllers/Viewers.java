package controllers;

import com.atlassian.connect.play.java.CheckValidOAuthRequest;
import com.atlassian.whoslooking.model.Viewables;
import com.atlassian.whoslooking.model.Viewer;

import play.libs.Crypto;

import play.Logger;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

public class Viewers extends Controller
{
    @BodyParser.Of(BodyParser.Json.class)
    public static Result put(String hostId, String resourceId)
    {
        Logger.info(String.format("Putting for host %s, resource %s", hostId, resourceId));

        if (request().body().isMaxSizeExceeded())
        {
            return badRequest("Don't flood me bro.");
        }

        Viewer newViewer;
        try
        {
            newViewer = extractViewerFromRequest();
        }
        catch (Exception e)
        {
            Logger.error("fail", e);
            return badRequest("Could not extract viewer information from request.");
        }

        if (!isValidRequestFromAuthenticatedUser(hostId, newViewer.name))
        {
            return badRequest("Don't spoof me bro.");
        }

        Viewables.putViewer(hostId, resourceId, newViewer);

        return ok(Json.toJson(Viewables.getViewers(hostId, resourceId)));
    }


    public static Result delete(String hostId, String resourceId, String userId)
    {
        Logger.info(String.format("Deleting for host %s, resource %s, user %s", hostId, resourceId, userId));

        if (!isValidRequestFromAuthenticatedUser(hostId, userId))
        {
            return badRequest("Don't spoof me bro.");
        }

        Viewer viewerToDelete = new Viewer();
        viewerToDelete.name = userId;

        Viewables.deleteViewer(hostId, resourceId, viewerToDelete);

        return noContent();
    }

    private static Viewer extractViewerFromRequest()
    {
        Viewer newViewer = Json.fromJson(request().body().asJson(), Viewer.class);
        newViewer.lastSeen = String.valueOf(System.currentTimeMillis());
        return newViewer;
    }

    private static boolean isValidRequestFromAuthenticatedUser(String hostId, String username)
    {
        // This should work, but doesn't. So we've rolled our own.
        // return username.equals(session().get("identity-on-"+hostId))

        Logger.info(String.format("Cookie key: " + "signed-identity-on-" + hostId));
        Logger.info(String.format("Cookies: %s", request().cookies()));
        Logger.info(String.format("Cookie: %s", request().cookies().get("signed-identity-on-"+hostId)));
        String signature = request().cookie("signed-identity-on-"+hostId).value();
        String expectedSignature = Crypto.sign(hostId+username);
        return expectedSignature.equals(signature);
    }

    @CheckValidOAuthRequest
    public static Result get(String hostId, String resourceId)
    {
        Logger.info(String.format("Returning list of viewers for '%s' on '%s'", resourceId, hostId));
        return ok(Json.toJson(Viewables.getViewers(hostId, resourceId)));
    }

    @CheckValidOAuthRequest
    public static Result list()
    {
        return ok(Json.toJson(Viewables.getAll()));
    }

}