package controllers;

import java.util.Map;

import com.atlassian.connect.play.java.AC;
import com.atlassian.connect.play.java.auth.jwt.AuthenticateJwtRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Supplier;

import org.apache.commons.lang3.StringUtils;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import service.*;
import utils.KeyUtils;

import static play.api.libs.Codecs.sha1;
import static service.AnalyticsService.*;

public class Poller extends Controller
{
    private final HeartbeatService heartbeatService = new RedisHeartbeatService();
    private final ViewerValidationService viewerValidationService = new RedisViewerValidationService();
    private final ViewerDetailsService viewerDetailsService = new ViewerDetailsService(heartbeatService);
    private final AnalyticsService analyticsService = new RedisAnalyticsService();
    private final MetricsService metricsService = new MetricsService();

    @AuthenticateJwtRequest
    public Result index() throws Exception
    {
        metricsService.incCounter("page-hit.poller");
        return metricsService.withMetric("poller", new Supplier<Result>() {
            @Override
            public Result get()
            {
                final String hostId = AC.getAcHost().getKey();
                final String resourceId = request().getQueryString("issue_key");
                final String accountId = AC.getUser().getOrNull();

                if (StringUtils.isBlank(accountId))
                {
                    return unauthorized(views.html.anonymous.render(hostId, resourceId, accountId));
                }

                heartbeatService.put(hostId, resourceId, accountId);
                analyticsService.fire(ACTIVE_HOST, sha1(hostId));
                analyticsService.fire(ACTIVE_USER, sha1(hostId)+":"+accountId);
                analyticsService.fireShortLived(PER_TENANT_PAGE_LOADS+"#"+sha1(hostId),
                        sha1(resourceId)+":"+sha1(accountId)+"@"+System.currentTimeMillis());

                String token = viewerValidationService.setToken(hostId, resourceId, accountId);

                final Map<String, JsonNode> viewersWithDetails = viewerDetailsService.getViewersWithDetails(resourceId, hostId);
                return ok(views.html.poller.render(Json.toJson(viewersWithDetails).toString(), hostId, resourceId, accountId, token));
            }
        });
    }
}