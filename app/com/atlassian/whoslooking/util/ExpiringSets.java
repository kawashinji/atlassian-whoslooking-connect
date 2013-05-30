package com.atlassian.whoslooking.util;

import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.SetMultimap;

import static com.google.common.collect.Multimaps.newSetMultimap;
import static com.google.common.collect.Multimaps.synchronizedSetMultimap;

public class ExpiringSets
{

    public static <V> Set<V> createWithCache(CacheBuilder cache)
    {
        return new ExpiringSet<V>(cache.build());
    }

    public static <K,V> SetMultimap<K,V> createExpiringSetMultimap(final CacheBuilder keyCache, final CacheBuilder valueCache)
    {
        SetMultimap<K,V> expiringSetMultiMap = synchronizedSetMultimap(newSetMultimap(keyCache.build().asMap(), new Supplier<Set<V>>() {

            @Override
            public Set<V> get()
            {
                return ExpiringSets.createWithCache(valueCache);
            }

        }));

        return expiringSetMultiMap;
    }

}
