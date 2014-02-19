package service;

import org.joda.time.DateTime;

public interface AnalyticsService
{
    String ACTIVE_USER = "active-user";
    String ACTIVE_HOST = "active-host";
    
    void fire(String eventName, String data);

    void gc();

    long count(String eventName, DateTime start, DateTime end);

}
