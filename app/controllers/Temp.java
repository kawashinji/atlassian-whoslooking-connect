package controllers;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import org.codehaus.jackson.JsonNode;

import play.Logger;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

public class Temp extends Controller
{

    @BodyParser.Of(BodyParser.Json.class)
    public static Result capabilities(String user)
    {
        JsonNode requestContent = request().body().asJson();

        Logger.info(requestContent.toString());

        String[] respondWith = request().queryString().get("respondWith");
        int status = 200;
        if (respondWith != null && respondWith.length != 0)
        {
            status = Integer.parseInt(respondWith[0]);
        }

        Map<String, String> response = ImmutableMap.of("user", user, "requestBody", requestContent.toString());

        return status(status, Json.toJson(response));
    }

}
