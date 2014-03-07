package com.atlassian.connect.play.java.oauth;

import com.atlassian.connect.play.java.BaseUrl;
import com.atlassian.connect.play.java.PublicKeyStore;
import com.atlassian.fugue.Option;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import net.oauth.*;
import net.oauth.signature.RSA_SHA1;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;

import static com.atlassian.connect.play.java.util.Utils.LOGGER;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

public final class OAuthRequestValidator<R>
{
    private final RequestHelper<R> requestHelper;
    private final PublicKeyStore publicKeyStore;
    private final BaseUrl baseUrl;

    /**
     * @param requestHelper the helper to extract information from the given type of request.
     * @param publicKeyStore the store to the public key, used to check the OAuth signature.
     * @param baseUrl the base URL of the remote app, this should return the same URL as the one found in the
     * {@code atlassian-remote-app.xml} descriptor.
     */
    public OAuthRequestValidator(RequestHelper<R> requestHelper, PublicKeyStore publicKeyStore, BaseUrl baseUrl)
    {
        this.requestHelper = checkNotNull(requestHelper);
        this.publicKeyStore = checkNotNull(publicKeyStore);
        this.baseUrl = checkNotNull(baseUrl);
    }

    /**
     * Validate the given request as an OAuth request. This method will return normally if the request is valid, it will
     * throw an exception otherwise.
     *
     * @param request the request to validate
     * @return the OAuth consumer key set in the request.
     * @throws InvalidOAuthRequestException if the request is invalid.
     */
    public String validate(R request)
    {
        final Multimap<String, String> parameters = getParameters(request);

        final String consumerKey = getConsumerKey(parameters);

        final OAuthMessage message = new OAuthMessage(
                requestHelper.getHttpMethod(request),
                requestHelper.getUrl(request, baseUrl),
                parameters.entries());
        try
        {
            final OAuthConsumer host = new OAuthConsumer(null, consumerKey, null, null);
            final String publicKey = publicKeyStore.getPublicKey(consumerKey);
            if (publicKey == null)
            {
                throw new UnknownAcHostException(consumerKey);
            }

            host.setProperty(RSA_SHA1.PUBLIC_KEY, publicKey);
            message.validateMessage(new OAuthAccessor(host), new SimpleOAuthValidator());

            return consumerKey;
        }
        catch (OAuthProblemException e)
        {
            LOGGER.warn("The request is not a valid OAuth request", e);
            throw new UnauthorisedOAuthRequestException(format("Validation failed: \nproblem: %s\nparameters: %s\n", e.getProblem(), e.getParameters()), e);
        }
        catch (OAuthException | IOException | URISyntaxException e)
        {
            LOGGER.error("An error happened validating the OAuth request.", e);
            throw new RuntimeException(e);
        }
    }

    private Multimap<String, String> getParameters(R request)
    {
        final ImmutableMultimap.Builder<String, String> parameters =
                ImmutableMultimap.<String, String>builder().putAll(requestHelper.getParameters(request));

        final Option<String> authorization = requestHelper.getHeader(request, "Authorization");
        if (authorization.isDefined())
        {
            for (OAuth.Parameter param : OAuthMessage.decodeAuthorization(authorization.get()))
            {
                parameters.put(param.getKey(), param.getValue());
            }
        }
        return parameters.build();
    }

    private String getConsumerKey(Multimap<String, String> parameters)
    {
        final Collection<String> consumerKeys = parameters.get("oauth_consumer_key");
        checkState(consumerKeys.size() == 1, "There should be only one value for the consumer key");
        String consumerKey = Iterables.getFirst(consumerKeys, null);
        LOGGER.debug("Found consumer key '" + consumerKey + "'.");
        return consumerKey;
    }
}
