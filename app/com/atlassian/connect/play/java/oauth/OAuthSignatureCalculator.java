package com.atlassian.connect.play.java.oauth;

import com.atlassian.connect.play.java.AC;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import com.ning.http.client.FluentStringsMap;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthServiceProvider;
import net.oauth.signature.RSA_SHA1;
import play.libs.WS;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.atlassian.connect.play.java.util.Utils.LOGGER;
import static java.lang.String.format;

public final class OAuthSignatureCalculator implements WS.SignatureCalculator
{
    private static final Supplier<OAuthConsumer> LOCAL_CONSUMER = Suppliers.memoize(new Supplier<OAuthConsumer>()
    {
        @Override
        public OAuthConsumer get()
        {
            return loadLocalConsumer();
        }
    });

    @Override
    public void sign(WS.WSRequest request)
    {
        final String authorizationHeaderValue = getAuthorizationHeaderValue(request);
        LOGGER.debug(format("Generated OAuth authorisation header: '%s'", authorizationHeaderValue));
        request.setHeader("Authorization", authorizationHeaderValue);
    }

    public String getAuthorizationHeaderValue(WS.WSRequest request) throws IllegalArgumentException
    {
        try
        {
            final OAuthConsumer localConsumer = LOCAL_CONSUMER.get();
            final Map<String, String> params = addOAuthParameters(localConsumer);
            addQueryParams(params, getQueryParams(request));

            final String method = request.getMethod();
            final String url = request.getUrl();

            LOGGER.debug("Creating OAuth signature for:");
            LOGGER.debug(format("Method: '%s'", method));
            LOGGER.debug(format("URL: '%s'", url));
            LOGGER.debug(format("Parameters: %s", params));

            final OAuthMessage oauthMessage = new OAuthMessage(method, url, params.entrySet());
            oauthMessage.sign(new OAuthAccessor(localConsumer));
            return oauthMessage.getAuthorizationHeader(null);
        }
        catch (OAuthException e)
        {
            // shouldn't really happen...
            throw new IllegalArgumentException("Failed to sign the request", e);
        }
        catch (IOException | URISyntaxException e)
        {
            // this shouldn't happen as the message is not being read from any IO streams, but the OAuth library throws
            // these around like they're candy, but far less sweet and tasty.
            throw new RuntimeException(e);
        }
    }

    private FluentStringsMap getQueryParams(WS.WSRequest request)
    {
        final Object underlyingRequest = getRequestObject(getRequestField(request), request);
        return getQueryParams(getGetQueryParamsMethod(underlyingRequest), underlyingRequest);
    }

    private FluentStringsMap getQueryParams(Method m, Object request)
    {
        try
        {
            return (FluentStringsMap) m.invoke(request);
        }
        catch (IllegalAccessException | InvocationTargetException e)
        {
            throw new RuntimeException(e);
        }
    }

    private Method getGetQueryParamsMethod(Object o)
    {
        try
        {
            final Method m = o.getClass().getMethod("getQueryParams");
            m.setAccessible(true);
            return m;
        }
        catch (NoSuchMethodException e)
        {
            throw new RuntimeException(e);
        }
    }

    private Field getRequestField(WS.WSRequest request)
    {
        try
        {
            final Field f = request.getClass().getSuperclass().getDeclaredField("request");
            f.setAccessible(true);
            return f;
        }
        catch (NoSuchFieldException e)
        {
            throw new RuntimeException(e);
        }
    }

    private Object getRequestObject(Field f, WS.WSRequest request)
    {
        try
        {
            return f.get(request);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void addQueryParams(Map<String, String> params, FluentStringsMap qp)
    {

        for (Map.Entry<String, List<String>> qparam : qp)
        {
            if (qparam.getValue().size() > 1)
            {
                throw new RuntimeException("Our OAuth library doesn't support multiple value query params!");
            }
            else if (!qparam.getValue().isEmpty())
            {
                params.put(qparam.getKey(), qparam.getValue().get(0));
            }
        }
    }

    private Map<String, String> addOAuthParameters(final OAuthConsumer local)
    {
        final HashMap<String, String> params = Maps.newHashMap();
        params.put(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.RSA_SHA1);
        params.put(OAuth.OAUTH_VERSION, "1.0");
        params.put(OAuth.OAUTH_CONSUMER_KEY, local.consumerKey);
        params.put(OAuth.OAUTH_NONCE, getNonce());
        params.put(OAuth.OAUTH_TIMESTAMP, getTimestamp());

        return params;
    }

    private String getNonce()
    {
        return System.nanoTime() + "";
    }

    private static String getTimestamp()
    {
        return System.currentTimeMillis() / 1000 + "";
    }

    private static OAuthConsumer loadLocalConsumer()
    {
        final OAuthServiceProvider serviceProvider = new OAuthServiceProvider(null, null, null);
        final OAuthConsumer localConsumer = new OAuthConsumer(null, AC.PLUGIN_KEY, null, serviceProvider);
        localConsumer.setProperty(RSA_SHA1.PRIVATE_KEY, OAuthKeys.privateKey.get());
        localConsumer.setProperty(RSA_SHA1.PUBLIC_KEY, OAuthKeys.publicKey.get());
        return localConsumer;
    }
}
