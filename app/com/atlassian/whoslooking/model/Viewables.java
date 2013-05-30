package com.atlassian.whoslooking.model;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import play.Play;

import com.atlassian.whoslooking.util.ExpiringSets;

import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
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

    private static SetMultimap<String, Viewer> store = ExpiringSets.createExpiringSetMultimap(issueExpiryConfig, viewerExpiryConfig);

    /**
     * @return active viewers of <code>id</code>
     */
    public static Set<Viewer> getViewers(final String id)
    {
        return store.get(id);
    }

    /**
     * @return map of all entities with active viewers to their active viewers.
     */
    public static ImmutableMap<String, Collection<Viewer>> getAll()
    {
        return ImmutableMap.copyOf(store.asMap());
    }

    public static void putViewer(String id, Viewer newViewer)
    {
        store.put(id,  newViewer);
    }

}
