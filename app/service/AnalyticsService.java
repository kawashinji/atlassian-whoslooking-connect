package service;

import org.joda.time.DateTime;

public interface AnalyticsService
{
    String ACTIVE_USER = "active-user";
    String ACTIVE_HOST = "active-host";
    
    String ACTIVE_USER_V2 = "active-user-v2";
    String ACTIVE_HOST_V2 = "active-host-v2";    
    
    void fire(String eventName, String data);

    void gc();

    long count(String eventName, DateTime start, DateTime end);

}
