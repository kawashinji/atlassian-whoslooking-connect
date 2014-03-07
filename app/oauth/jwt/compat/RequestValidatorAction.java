package oauth.jwt.compat;

import com.atlassian.connect.play.java.AC;
import com.atlassian.connect.play.java.AcHost;
import com.atlassian.connect.play.java.PublicKeyStore;
import com.atlassian.connect.play.java.auth.InvalidAuthenticationRequestException;
import com.atlassian.connect.play.java.auth.jwt.JwtAuthConfig;
import com.atlassian.connect.play.java.auth.jwt.JwtAuthenticationResult;
import com.atlassian.connect.play.java.oauth.OAuthRequestValidator;
import com.atlassian.connect.play.java.oauth.PlayRequestHelper;
import com.atlassian.jwt.Jwt;
import com.atlassian.jwt.core.http.auth.JwtAuthenticator;

import com.google.common.base.Function;

import play.Logger;
import play.libs.F.Either;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.SimpleResult;

public final class RequestValidatorAction extends Action.Simple
{
    private final OAuthRequestValidator<Http.Request> validator = new OAuthRequestValidator<>(new PlayRequestHelper(), new PlayPublicKeyStore(), AC.baseUrl);
    private static final JwtAuthenticator<Request, Response, JwtAuthenticationResult> authenticator = JwtAuthConfig.getJwtAuthenticator();

    @Override
    public Promise<SimpleResult> call(Http.Context context) throws Throwable
    {
        try
        {
            return new AuthenticationHelper().authenticate(context, delegate);
        }
        catch (Exception e)
        {
            Logger.info("Could not validate JWT, falling back to OAuth.");
            AC.setAcHost(validator.validate(context.request()));
            AC.refreshToken(false);

            return delegate.call(context);
        }

    }

    private static final class PlayPublicKeyStore implements PublicKeyStore
    {
        @Override
        public String getPublicKey(String consumerKey)
        {
            return AC.getAcHost(consumerKey).map(new Function<AcHost, String>()
            {
                @Override
                public String apply(AcHost host)
                {
                    return host.getPublicKey();
                }
            }).getOrNull();
        }
    }


    // exists to make it easier to test
    static class AuthenticationHelper {
        public Promise<SimpleResult> authenticate(Context context, Action<?> delegate) throws Throwable
        {
            try
            {
                Either<Status, Jwt> authResult = authenticator.authenticate(context.request(), context.response()).getResult();
                if (authResult.left.isDefined()) {
                    return Promise.pure((SimpleResult)authResult.left.get());
                }

                Jwt jwt = authResult.right.get();
                AC.setAcHost(jwt.getIssuer());
                AC.setUser(jwt.getSubject());
                AC.refreshToken(false);

                return delegate.call(context);
            }
            catch (InvalidAuthenticationRequestException e)
            {
                return Promise.pure((SimpleResult)badRequest("Bad request: " + e.getMessage()));
            }
        }

    }
}
