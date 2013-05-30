package controllers;

import java.util.concurrent.TimeUnit;

import com.atlassian.connect.play.java.AC;
import com.atlassian.connect.play.java.AcHost;
import com.atlassian.connect.play.java.CheckValidOAuthRequest;
import com.atlassian.connect.play.java.oauth.OAuthSignatureCalculator;
import com.atlassian.fugue.Option;

import com.google.common.base.Suppliers;

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
import play.mvc.Http;
import play.mvc.Result;

import static com.atlassian.connect.play.java.oauth.OAuthSignatureCalculator.USER_ID_QUERY_PARAMETER;
import static com.atlassian.connect.play.java.util.Utils.LOGGER;
import static java.lang.String.format;

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

//        Object value = Cache.get(hostId + "-" + userName + "-details");
//        if (value == null)
//        {
//            Logger.info("Calling...");
//            Promise<Response> promise = myACurl("/rest/api/latest/user").get();
//
//            promise.onRedeem(new Callback<WS.Response>()
//            {
//
//                @Override
//                public void invoke(Response a) throws Throwable
//                {
//                    JsonNode asJson = a.asJson();
//                    if (!asJson.has("errorMessages"))
//                    {
//                        Cache.set(hostId + "-" + userName + "-details", asJson.toString(), 1200);
//                    }
//                    else
//                    {
//                        Logger.error(asJson.toString());
//                    }
//                }
//            });
//
//            promise.recover(new Function<Throwable, WS.Response>()
//            {
//                @Override
//                public WS.Response apply(Throwable t)
//                {
//                    Logger.error("An error occurred", t);
//                    // Can't really recover from this, so just rethrow.
//                    throw new RuntimeException(t);
//                }
//            });
//        }

        return ok(views.html.poller.render());
    }

    public static WS.WSRequestHolder myACurl(String url)
    {

        final AcHost acHost = (AcHost) Http.Context.current().args.get("ac_host");
        final Option<String> user = AC.getUser();

        final String absoluteUrl = acHost.getBaseUrl() + url;

        LOGGER.debug(format("Creating request to '%s'", absoluteUrl));

        final WS.WSRequestHolder request = WS.url(absoluteUrl)
                                             .setTimeout((int) TimeUnit.SECONDS.convert(5, TimeUnit.MILLISECONDS))
                                             .setFollowRedirects(false) // because we need to sign again in those cases.
                                             .sign(new OAuthSignatureCalculator(user));

        return user.fold(Suppliers.ofInstance(request), new com.google.common.base.Function<String, WS.WSRequestHolder>()
        {
            @Override
            public WS.WSRequestHolder apply(String user)
            {
                return request.setQueryParameter(USER_ID_QUERY_PARAMETER, user);
            }
        });

    }



}