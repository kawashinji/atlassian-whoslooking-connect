package controllers;

import com.atlassian.connect.play.java.controllers.AcController;

import com.google.common.base.Supplier;

import play.mvc.Controller;
import play.mvc.Result;

public class Application extends Controller
{

    public static Result index()
    {
        // serve the descriptor when accept header is 'application/xml', try
        // 'curl -H "Accept: application/xml" http://localhost:9000'
        return AcController.index(home(), descriptor());
    }

    private static Supplier<Result> descriptor()
    {
        return new Supplier<Result>()
        {
            @Override
            public Result get()
            {
                return ok(views.xml.descriptor.render());
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
