package controllers;

import com.atlassian.connect.play.java.controllers.AcController;

import com.google.common.base.Supplier;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;

import service.ApiMigrationsService;
import service.MetricsService;

import utils.JWTUtils;

import java.util.concurrent.TimeUnit;

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
        Logger.info("Payload includes a JWT token - let's ensure qsh is valid.");
        if (!migrationsService.validateQsh(request(), jwtString)) {
            return status(403, "Install failed (qsh validation failure).");
        }

        if (!migrationsService.validateSignedInstall(request(), jwtString)) {
            return status(403, "SignatureVerificationException: cannot verify the signature for signed install");
        }

        return AcController.registration().get(8000, TimeUnit.MILLISECONDS);
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
