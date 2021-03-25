package controllers;

import com.atlassian.connect.play.java.controllers.AcController;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;
import service.MetricsService;
import service.RedisViewerValidationService;
import service.ViewerValidationService;

import java.util.List;

public class FrontEndMetrics extends Controller
{

    private final MetricsService metricsService = new MetricsService();
    private final List<String> validKeys = new ImmutableList.Builder()
            .add("display-name-fetch-success", "display-name-fetch-fail")
            .build();

    public Result put(final String key) {
        Logger.trace("received fe metric " + key);
        if (!validKeys.contains(key)) {
            return notFound();
        }

        metricsService.incCounter("front-end." + key);
        return ok();
    }

}
