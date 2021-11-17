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

import static play.api.libs.Codecs.sha1;
import static service.AnalyticsService.ACTIVE_HOST;
import static service.AnalyticsService.ACTIVE_USER;

public class Poller extends Controller
{
    private final HeartbeatService heartbeatService = new RedisHeartbeatService();
    private final ViewerValidationService viewerValidationService = new RedisViewerValidationService();
    private final ViewerDetailsService viewerDetailsService = new ViewerDetailsService(heartbeatService);
    private final AnalyticsService analyticsService = new RedisAnalyticsService();
    private final MetricsService metricsService = new MetricsService();

    //3. Verify OAuth header from Jira to Whoslooking????? need to find out with downloading source code
    @AuthenticateJwtRequest
    public Result index() throws Exception
    {
        metricsService.incCounter("page-hit.poller");
        return metricsService.withMetric("poller", new Supplier<Result>() {
            @Override
            public Result get()
            {
                //4. Get host/issue/account id
                final String hostId = AC.getAcHost().getKey();
                final String resourceId = request().getQueryString("issue_key");
                final String accountId = AC.getUser().getOrNull();

                if (StringUtils.isBlank(accountId))
                {
                    return unauthorized(views.html.anonymous.render(hostId, resourceId, accountId));
                }

                // 6. save user to heartbeat list
                heartbeatService.put(hostId, resourceId, accountId);
                analyticsService.fire(ACTIVE_HOST, sha1(hostId));
                analyticsService.fire(ACTIVE_USER, sha1(hostId)+":"+accountId);

                //8. Generate ID token for host +account+ issueId
                // This gonna expire after 30 seconds
                String token = viewerValidationService.setToken(hostId, resourceId, accountId);

                //9.1. Get user details
                final Map<String, JsonNode> viewersWithDetails = viewerDetailsService.getViewersWithDetails(resourceId, hostId);

                //9.2 responde with content
                return ok(views.html.poller.render(Json.toJson(viewersWithDetails).toString(), hostId, resourceId, accountId, token));
            }   
        });
    }
}