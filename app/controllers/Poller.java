package controllers;

import org.codehaus.jackson.JsonNode;

import play.Logger;
import play.api.libs.Crypto;
import play.cache.Cache;
import play.libs.F.Callback;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;
import play.mvc.Controller;
import play.mvc.Result;

import com.atlassian.connect.play.java.AC;
import com.atlassian.connect.play.java.CheckValidOAuthRequest;

public class Poller extends Controller
{

    @CheckValidOAuthRequest
    public static Result index() throws Exception
    {
        final String hostId = request().queryString().get("oauth_consumer_key")[0];
        final String userName = AC.getUser().getOrNull();

        // TODO: this session() call doesn't work (ends up empty on poll calls).. so we have to roll our own.
        session("identity-on-" + hostId, userName);
        response().setCookie("identity-on-" + hostId, userName);
        response().setCookie("signed-identity-on-" + hostId, Crypto.sign(hostId + userName));

        Object value = Cache.get(hostId + "-" + userName + "-details");
        if (value == null)
        {
            Logger.info("Calling...");
            Promise<Response> promise = AC.url("/rest/api/latest/user")
            		.setQueryParameter("username", userName) .get();

            promise.onRedeem(new Callback<WS.Response>()
            {

                @Override
                public void invoke(Response a) throws Throwable
                {
                    JsonNode asJson = a.asJson();
                    if (!asJson.has("errorMessages"))
                    {
                        Cache.set(hostId + "-" + userName + "-details", asJson.toString(), 1200);
                    }
                    else
                    {
                        Logger.error(asJson.toString());
                    }
                }
            });

            promise.recover(new Function<Throwable, WS.Response>()
            {
                @Override
                public WS.Response apply(Throwable t)
                {
                    Logger.error("An error occurred", t);
                    // Can't really recover from this, so just rethrow.
                    throw new RuntimeException(t);
                }
            });
        }

        return ok(views.html.poller.render());
    }


}