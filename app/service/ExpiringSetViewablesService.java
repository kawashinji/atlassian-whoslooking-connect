package service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import com.atlassian.whoslooking.util.ExpiringSets;

import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;

import org.codehaus.jackson.JsonNode;

import play.Play;

/**
* Provides an implementation of ViewablesService backed by an in-memory ExpiringSet.
*/
public class ExpiringSetViewablesService implements ViewablesService
{

    private static final Long VIEWER_EXPIRY_SECONDS = Long.valueOf(Play.application()
                                          .configuration()
                                          .getLong("whoslooking.viewer-expiry.seconds", 10L));

    private static final CacheBuilder issueExpiryConfig = CacheBuilder.newBuilder().maximumSize(10000)
                                                                      .expireAfterAccess(1, TimeUnit.HOURS);

    private static final CacheBuilder viewerExpiryConfig = CacheBuilder.newBuilder()
                                                                       .maximumSize(100)
                                                                       .expireAfterWrite(VIEWER_EXPIRY_SECONDS,
                                                                                         TimeUnit.SECONDS);

    private static SetMultimap<String, String> store = ExpiringSets.createExpiringSetMultimap(issueExpiryConfig,
                                                                                              viewerExpiryConfig);

    @Override
    public Set<String> getViewers(final String hostId, final String resourceId)
    {
        String key = buildKey(hostId, resourceId);
        return store.get(key);
    }

    private String buildKey(final String hostId, final String resourceId)
    {
        return hostId + '-' + resourceId + '-' ;
    }

    @Override
    public  void putViewer(final String hostId, final String resourceId, final String newViewer)
    {
        String key = buildKey(hostId, resourceId);
        store.put(key, newViewer);
    }

    @Override
    public void deleteViewer(final String hostId, final String resourceId, final String viewer)
    {
        String key = buildKey(hostId, resourceId);
        store.remove(key, viewer);
    }

    @Override
    public Map<String, JsonNode> getViewersWithDetails(final String resourceId, final String hostId)
    {
        Map<String, JsonNode> viewersWithDetails = Maps.asMap(this.getViewers(hostId, resourceId),
                                                              new Function<String, JsonNode>()
                                                              {
                                                                  @Override
                                                                  @Nullable
                                                                  public JsonNode apply(@Nullable String viewerName)
                                                                  {
                                                                      return ViewerDetailsService.getCachedDetailsFor(hostId, viewerName);
                                                                  }
                                                              });
        return viewersWithDetails;
    }

}
