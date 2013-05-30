package com.atlassian.whoslooking.model;

import org.joda.time.DateTime;

import com.google.common.base.Objects;

public class Viewer
{
    public String name;
    public String lastSeen;
    
    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                       .add("name", name)
                       .add("lastSeen", lastSeen)
                       .toString();
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof Viewer
                && Objects.equal(name, ((Viewer)obj).name);
    }
    
    
    
}