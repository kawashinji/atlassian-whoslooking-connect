package controllers;

import com.atlassian.connect.play.java.auth.jwt.*;
import com.atlassian.connect.play.java.controllers.AcController;

import com.atlassian.jwt.core.reader.NimbusJwtReaderFactory;
import com.google.common.base.Supplier;

import org.apache.commons.lang.StringUtils;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import service.MetricsService;

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

    @AuthenticateJwtRequest
    public static Result register()
    {
        new MetricsService().incCounter("page-hit.install");
        Logger.info("In install override");
        Logger.trace(Controller.request().getHeader("Authorization"));

        String jwtString = extractJwt(Controller.request());

        Logger.trace(jwtString);

        return AcController.registration().get();
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
