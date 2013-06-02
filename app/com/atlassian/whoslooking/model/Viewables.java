package com.atlassian.whoslooking.model;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.codehaus.jackson.JsonNode;

import play.Play;
import service.ViewerDetailsService;

import com.atlassian.whoslooking.util.ExpiringSets;

import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;

public class Viewables
{

    private static final CacheBuilder issueExpiryConfig = CacheBuilder.newBuilder()
                                                            .maximumSize(10000)
                                                            .expireAfterAccess(1, TimeUnit.HOURS);

    private static final CacheBuilder viewerExpiryConfig = CacheBuilder.newBuilder()
                                                                .maximumSize(100)
                                                                .expireAfterWrite(Long.valueOf(Play.application().configuration().getLong("whoslooking.viewer-expiry.seconds", 10L)),
                                                                                  TimeUnit.SECONDS);

    
    private static SetMultimap<String, String> store = ExpiringSets.createExpiringSetMultimap(issueExpiryConfig, viewerExpiryConfig);

    /**
     * @return active viewers of <code>id</code>
     */
    public static Set<String> getViewers(final String hostId, final String resourceId)
    {
        String key = buildKey(hostId, resourceId);
        return store.get(key);
    }

    private static String buildKey(final String hostId, final String resourceId)
    {
        return hostId + '-' + resourceId;
    }

    /**
     * @return map of all entities with active viewers to their active viewers.
     */
    public static ImmutableMap<String, Collection<String>> getAll()
    {
        return ImmutableMap.copyOf(store.asMap());
    }

    public static void putViewer(final String hostId, final String resourceId, final String newViewer)
    {
        String key = buildKey(hostId, resourceId);
        store.put(key,  newViewer);
    }

    public static void deleteViewer(final String hostId, final String resourceId, final String viewer)
    {
        String key = buildKey(hostId, resourceId);
        store.remove(key, viewer);
    }
    
	public static Map<String, JsonNode> getViewersWithDetails(final String resourceId, final String hostId) {
		Map<String, JsonNode> viewersWithDetails = Maps.asMap(Viewables.getViewers(hostId, resourceId),
        		new Function<String, JsonNode>() {
			@Override
			@Nullable
			public JsonNode apply(@Nullable String viewerName) {
				return ViewerDetailsService.getCachedDetailsFor(hostId, viewerName);				
			}        	
        });
		return viewersWithDetails;
	}

}
