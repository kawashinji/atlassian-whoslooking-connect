package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import org.apache.commons.lang.StringUtils;

import com.sun.org.apache.xerces.internal.util.URI;

import models.*;

public class Poller extends Controller
{

    public static Result index()
    {
        return ok(views.html.poller.render());
    }

}