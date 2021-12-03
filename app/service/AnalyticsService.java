package service;

import org.joda.time.DateTime;

import java.util.LongSummaryStatistics;

public interface AnalyticsService
{
    String ACTIVE_USER = "active-user-v2";
    String ACTIVE_HOST = "active-host-v2";
    String PER_TENANT_PAGE_LOADS = "per-tenant-page-loads";
    String PER_TENANT_POLL_EVENTS = "per-tenant-poll-events";

    void fire(String eventName, String data);

    // Ensure we can make high-cardinality events short-lived so we don't fill up redis.
    void fireShortLived(String eventName, String data);

    void gc();

    long count(String eventName, DateTime start, DateTime end);

    LongSummaryStatistics getStats(String category, DateTime start, DateTime end);
}
