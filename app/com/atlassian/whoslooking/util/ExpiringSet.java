package com.atlassian.whoslooking.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.google.common.cache.Cache;

/**
* A <code>Set</code> with entries that expire.
* <p>
* Values are held in memory and backed by <code>com.google.common.cache.Cache</code>.
*/
public class ExpiringSet<V> implements Set<V>
{
    // The Long value is pretty much a placeholder. We really just use the Cache's "keyset".
    // This might be an abuse of Guava caches.
    private final Cache<V, Long> cache;

    ExpiringSet(Cache<V, Long> cache)
    {
        this.cache = cache;
    }

    private Collection<V> delegateSet()
    {
        return cache.asMap().keySet();
    }

    @Override
    public int size()
    {
        return delegateSet().size();
    }

    @Override
    public boolean isEmpty()
    {
        return delegateSet().isEmpty();
    }

    @Override
    public boolean contains(Object o)
    {
        return delegateSet().contains(o);
    }

    @Override
    public Iterator<V> iterator()
    {
        return delegateSet().iterator();
    }

    @Override
    public Object[] toArray()
    {
        return delegateSet().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a)
    {
        return delegateSet().toArray(a);
    }

    @Override
    public boolean add(V e)
    {
        Long originalValue = cache.getIfPresent(e);

        cache.put(e, 0L);

        // not thread safe
        return originalValue == null;
    }

    @Override
    public boolean remove(Object o)
    {
        try
        {
            Long originalValue = cache.getIfPresent(o);

            cache.invalidate(o);
            delegateSet().remove(o);

            // not thread safe
            return originalValue != null;
        }
        catch (ClassCastException e)
        {
            return false;
        }
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        return delegateSet().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends V> c)
    {
        boolean agglom = false;

        for (V v : c)
        {
            agglom = add(v) || agglom;
        }

        return agglom;
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        boolean agglom = false;

        for (Object o : c)
        {
            agglom = remove(o) || agglom;
        }

        return agglom;
    }

    @Override
    public void clear()
    {
        cache.invalidateAll();
    }

    @Override
    public int hashCode()
    {
        return delegateSet().hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        return delegateSet().equals(obj);
    }

    @Override
    public String toString()
    {
        return delegateSet().toString();
    }

}