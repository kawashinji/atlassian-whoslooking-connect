package utils;

import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;

import com.google.common.base.Supplier;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import net.minidev.json.JSONObject;

import play.Logger;
import play.libs.WS;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import com.atlassian.connect.play.java.AC;
import com.atlassian.connect.play.java.auth.jwt.*;
import com.atlassian.connect.play.java.controllers.AcController;
import com.atlassian.jwt.CanonicalHttpRequest;
import com.atlassian.jwt.core.HttpRequestCanonicalizer;
import com.atlassian.jwt.core.SimpleJwt;
import com.atlassian.jwt.core.reader.NimbusJwtReaderFactory;

import static play.libs.F.Function;
import static play.libs.F.Promise;

public class JWTUtils {

    public static String fetchRSAPublicKey(String kid) {
        // get connect_keys_uri from config
        // return the rsa key by calling it
        String hostURL = "https://cs-migrations--cdn.us-west-1.staging.public.atl-paas.net/" + kid;
        Promise<WS.Response> result = WS.url(hostURL).get().get(5000); //timeout
        result.sync();
    }

    public String decodeProtectedHeader(String jwtToken) {
        return "";
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

    public static String decodeStringFromBase64Url(String base64String) {
        byte[] decodedBytes = Base64.getDecoder().decode(base64String);
        return new String(decodedBytes);
    }

}
