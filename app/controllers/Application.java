package controllers;

import com.atlassian.connect.play.java.auth.jwt.AuthenticateJwtRequest;
import com.atlassian.connect.play.java.controllers.AcController;

import com.google.common.base.Supplier;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;

import service.MetricsService;

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
