package controllers;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;

public class Temp extends Controller
{

    public static Result capabilities()
    {
        Logger.info(request().body().asText());
        return ok();
    }

}
