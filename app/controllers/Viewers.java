package controllers;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.atlassian.connect.play.java.CheckValidOAuthRequest;
import com.atlassian.fugue.Pair;
import com.atlassian.whoslooking.model.Viewables;
import com.atlassian.whoslooking.model.Viewer;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import play.cache.Cache;
import play.libs.Crypto;

import play.Logger;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import service.ViewerDetailsService;

public class Viewers extends Controller
{
    @BodyParser.Of(BodyParser.Json.class)
    public static Result put(final String hostId_brokenOnUnicorn, final String resourceId)
    {
        // Path parsing for hostId failing on Unicorn, why? Have to pass it as a parameter for now.        
        final String hostId = (hostId_brokenOnUnicorn != null) ? hostId_brokenOnUnicorn : request().getQueryString("hostId_for_unicorn");

        Logger.debug(String.format("Putting for host %s, resource %s", hostId, resourceId));

        if (request().body().isMaxSizeExceeded())
        {
            return badRequest("Don't flood me bro.");
        }

        final Viewer newViewer;
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

        Set<String> viewerNames = ImmutableSet.copyOf(Collections2.transform(Viewables.getViewers(hostId, resourceId),
        		new Function<Viewer, String>() {
					@Override
					@Nullable
					public String apply(Viewer viewer) {
						return viewer.name;
					}
        }));
        
        Map<String, String> viewersWithDetails = Maps.asMap(viewerNames, new Function<String, String>() {

			@Override
			@Nullable
			public String apply(@Nullable String viewerName) {
				return new ViewerDetailsService().getDetailsFor(hostId, viewerName);
			}
        	
        });
        
        return ok(Json.toJson(viewersWithDetails));
    }


    public static Result delete(String hostId, String resourceId, String userId)
    {
        Logger.debug(String.format("Deleting for host %s, resource %s, user %s", hostId, resourceId, userId));

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

        Logger.debug(String.format("Cookie key: " + "signed-identity-on-" + hostId));
        Logger.debug(String.format("Cookies: %s", request().cookies()));
        Logger.debug(String.format("Cookie: %s", request().cookies().get("signed-identity-on-"+hostId)));
        String signature = request().cookie("signed-identity-on-"+hostId).value();
        String expectedSignature = Crypto.sign(hostId+username);
        return expectedSignature.equals(signature);
    }

    @CheckValidOAuthRequest
    public static Result get(String hostId, String resourceId)
    {
        Logger.debug(String.format("Returning list of viewers for '%s' on '%s'", resourceId, hostId));
        return ok(Json.toJson(Viewables.getViewers(hostId, resourceId)));
    }

    @CheckValidOAuthRequest
    public static Result list()
    {
        return ok(Json.toJson(Viewables.getAll()));
    }

}