package utils;

import play.libs.WS;
import play.mvc.Http;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;


public class JWTUtils {

    public static RSAPublicKey fetchRSAPublicKey(String kid) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        // get connect_keys_uri from config
        // return the rsa key by calling it
        String hostURL = "https://cs-migrations--cdn.us-west-1.staging.public.atl-paas.net/" + kid;
        WS.Response response = WS.url(hostURL).get().get(5000, TimeUnit.MILLISECONDS); //timeout
        String encodedKey = response.getBody();
        String pubKeyPEM = encodedKey.replace("-----BEGIN PUBLIC KEY-----\n", "").replace("-----END PUBLIC KEY-----", "");
        byte[] encodedPublicKey = Base64.getDecoder().decode(pubKeyPEM);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(encodedPublicKey);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(spec);
    }

    public static String extractJwt(Http.Request request) {
        // if it is not sent as a query param, check for the header
        String jwt = getJwtParameter(request);
        if (jwt == null) {
            jwt = getJwtHeaderValue(request);
        }

        return jwt;
    }

    private static String getJwtParameter(Http.Request request) {
        String jwtParam = request.getQueryString("jwt");
        return jwtParam == null || jwtParam.isEmpty() ? null : jwtParam;
    }

    private static String getJwtHeaderValue(Http.Request request) {
        Iterable<String> headers = Collections.singleton(request.getHeader("Authorization"));
        Iterator i$ = headers.iterator();

        String authzHeader;
        String first4Chars;
        do {
            if (!i$.hasNext()) {
                return null;
            }

            String header = (String)i$.next();
            if (null == header) {
                return null;
            }
            authzHeader = header.trim();
            first4Chars = authzHeader.substring(0, Math.min(4, authzHeader.length()));
        } while(!"JWT ".equalsIgnoreCase(first4Chars));

        return authzHeader.substring(4);
    }
}
