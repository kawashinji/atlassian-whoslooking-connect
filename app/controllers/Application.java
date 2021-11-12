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

import service.ApiMigrationsService;
import service.MetricsService;

import utils.JWTUtils;

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
        final ApiMigrationsService migrationsService = new ApiMigrationsService();
        Logger.info("In install override");

        String jwtString = JWTUtils.extractJwt(Controller.request());
        Logger.info("jwt string: " + jwtString);
        if (jwtString != null) {
            Logger.info("Payload includes a JWT token - let's ensure qsh is valid.");
            if (!migrationsService.validateQsh(request(), jwtString)) {
                return status(403, "Install failed (qsh validation failure).");
            }

        } else {
            Logger.info("No JWT token - must be first time install. Verify if it is a signed install.");
            try {
                migrationsService.validateSignedInstall(request(), jwtString);
            } catch (Exception e) {
                Logger.error("Signature verification failed", e);
                return status(403, "SignatureVerificationException: cannot verify the signature for signed install");
            }
        }

        return AcController.registration().get();
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
