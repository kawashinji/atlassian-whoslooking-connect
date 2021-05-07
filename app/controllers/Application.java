package controllers;

import com.atlassian.connect.play.java.AC;
import com.atlassian.connect.play.java.auth.jwt.*;
import com.atlassian.connect.play.java.controllers.AcController;

import com.atlassian.jwt.CanonicalHttpRequest;
import com.atlassian.jwt.core.HttpRequestCanonicalizer;
import com.atlassian.jwt.core.SimpleJwt;
import com.atlassian.jwt.core.reader.NimbusJwtReaderFactory;
import com.google.common.base.Supplier;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import net.minidev.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import service.MetricsService;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Collections;
import java.util.Iterator;

public class Application extends Controller
{

    public static Result index()
    {
        new MetricsService().incCounter("page-hit.home");
        // serve the descriptor when accept header is 'application/xml', try
        // 'curl -H "Accept: application/xml" http://localhost:9000'
        return AcController.index(home(), descriptor());
    }

    /**
     * Purely for qsh check on install event (VULN-298834)
     */
    public static Result register()
    {
        new MetricsService().incCounter("page-hit.install");
        Logger.info("In install override");

        String jwtString = extractJwt(Controller.request());
        Logger.info("jwt string: " + jwtString);
        if (jwtString != null) {
            Logger.info("Payload includes a JWT token - let's ensure qsh is valid.");
            if (!validateQsh(jwtString)) {
                return status(403, "Install failed (qsh validation failure).");
            }
        } else {
            Logger.info("No JWT token - must be first time install. No check required.");
        }

        return AcController.registration().get();
    }

    private static boolean validateQsh(String jwtString) {
        try {
            PlayRequestWrapper wrappedReq = new PlayRequestWrapper(request(), (new URL((String) AC.baseUrl.get())).getPath());
            CanonicalHttpRequest cannonicalRequest = wrappedReq.getCanonicalHttpRequest();
            Logger.info("Cannonical request: " + HttpRequestCanonicalizer.canonicalize(cannonicalRequest));

            JWSObject jwso = JWSObject.parse(jwtString);
            JSONObject payload = jwso.getPayload().toJSONObject();
            if (payload.get("qsh") == null) {
                Logger.error("qsh missing from payload");
                return false;
            }

            String qsh = payload.get("qsh").toString();
            Logger.info("Input qsh value: " + qsh);
            String computedHash = HttpRequestCanonicalizer.computeCanonicalRequestHash(cannonicalRequest);
            Logger.info("Computed qsh value: " + computedHash);

            if (!computedHash.equals(qsh)) {
                Logger.error("qsh check failure: [computed: " + computedHash + "]; [qsh: " + qsh +"]");
                return false;
            }

        } catch (Exception e) {
            Logger.error("Failed to parse install JWT token", e);
            return false;
        }

        return true;
    }

    public static String extractJwt(Http.Request request) {
        String jwt = getJwtParameter(request);
        if (jwt == null) {
            jwt = getJwtHeaderValue(request);
        }

        return jwt;
    }

    private static String getJwtParameter(Http.Request request) {
        String jwtParam = request.getQueryString("jwt");
        return StringUtils.isEmpty(jwtParam) ? null : jwtParam;
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

    private static Supplier<Result> descriptor()
    {
        return new Supplier<Result>()
        {
            @Override
            public Result get()
            {
                return AcController.descriptor();
            }
        };
    }

    private static Supplier<Result> home()
    {
        return new Supplier<Result>()
        {
            @Override
            public Result get()
            {
                return ok(views.html.home.render());
            }
        };
    }


}
