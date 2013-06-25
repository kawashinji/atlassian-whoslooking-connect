package controllers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;

import com.atlassian.connect.play.java.oauth.PlayRequestHelper;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import org.codehaus.jackson.JsonNode;

import play.mvc.Http;

import net.oauth.OAuthException;

import net.oauth.SimpleOAuthValidator;

import net.oauth.OAuthConsumer;

import net.oauth.OAuthAccessor;

import net.oauth.OAuthMessage;

import net.oauth.OAuthValidator;

import play.libs.OAuth;

import play.Logger;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

public class Temp extends Controller
{

    private static final String consumerSecret="mySecret";
    private static final String consumerKey="myKey";

    @BodyParser.Of(BodyParser.Json.class)
    public static Result capabilities(String user) throws IOException, URISyntaxException
    {
        JsonNode requestContent = request().body().asJson();

        Logger.info("Request headers: " + request().headers());
        Logger.info("Request qs: " + request().queryString());

        int status = getResponseStatus();
        String oauthResult = attemptOAuthValidation();
        Map<String, String> response = ImmutableMap.of("oauthResult", oauthResult, "user", user, "requestBody", requestContent.toString());

        Logger.info(response.toString());

        return status(status, Json.toJson(response));
    }

    private static String attemptOAuthValidation() throws IOException, URISyntaxException
    {
        OAuthMessage message = new OAuthMessage(request().method(), request().uri(), getParameters(request()));
        OAuthConsumer consumer=new OAuthConsumer(null, consumerKey, consumerSecret, null);
        OAuthAccessor accessor=new OAuthAccessor(consumer);

        SimpleOAuthValidator validator=new SimpleOAuthValidator();
        String oauthValidated;
        try
        {
            validator.validateMessage(message, accessor);
            oauthValidated = "true";
        }
        catch (OAuthException e)
        {
            oauthValidated = "false: " + e.getMessage();
        }
        return oauthValidated;
    }

    private static int getResponseStatus()
    {
        String[] respondWith = request().queryString().get("respondWith");
        int status = 200;
        if (respondWith != null && respondWith.length != 0)
        {
            status = Integer.parseInt(respondWith[0]);
        }
        return status;
    }

    private static Collection<? extends Map.Entry> getParameters(Http.Request request)
    {
        final Multimap<String, String> map = ArrayListMultimap.create();
        for (Map.Entry<String, String[]> entry : request.queryString().entrySet())
        {
            for (String v : entry.getValue())
            {
                final String k = entry.getKey();
                map.put(k, v);
            }
        }
        for (Map.Entry<String, String[]> entry : request.headers().entrySet())
        {
            for (String v : entry.getValue())
            {
                final String k = entry.getKey();
                map.put(k, v);
            }
        }
        return map.entries();
    }

}
