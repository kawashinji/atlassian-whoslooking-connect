package controllers;

import com.atlassian.whoslooking.model.Viewables;
import com.atlassian.whoslooking.model.Viewer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import play.mvc.BodyParser;

import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

public class Viewers extends Controller
{
    // Gson claims to be thread-safe.
    private static Gson gson = new GsonBuilder().create();

    public static Result index()
    {
        return null;
    }

    @BodyParser.Of(BodyParser.Json.class)
    public static Result put(String viewableUrl)
    {
        try
        {
            // TODO: need to stream request body?
            //Viewer newViewer = gson.fromJson(request().body().asJson().asText(), Viewer.class);
            Viewer newViewer = Json.fromJson(request().body().asJson(), Viewer.class);
            validateRequest();
            Viewables.putViewer(viewableUrl, newViewer);
        }
        catch (Exception e)
        {
            Logger.error("fail", e);
            return badRequest("Could not extract viewer information from request: " + e);
        }

        return ok(Json.toJson(Viewables.getViewers(viewableUrl)));
    }

    private static void validateRequest()
    {
        // todo
    }

    public static Result get(String id)
    {
        validateRequest();
        Logger.info(String.format("Returning list of viewers for '%s'", id));
        return ok(Json.toJson(Viewables.getViewers(id)));
    }

    public static Result list()
    {

        return ok(Json.toJson(Viewables.getAll()));
    }

}