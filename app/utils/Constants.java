package utils;

import java.util.concurrent.TimeUnit;

public class Constants {

    // A viewer is considered to not be looking anymore if no heartbeat has been received in this amount for time.
    public static final String VIEWER_EXPIRY_SECONDS = "whoslooking.viewer-expiry.seconds";
    public static final int VIEWER_EXPIRY_SECONDS_DEFAULT = (int) TimeUnit.MINUTES.toMillis(5);

    // A viewer set associated with an issue is purged if no one has requested it for this amount of time.
    public static final String VIEWER_SET_EXPIRY_SECONDS = "whoslooking.viewer-set-expiry.seconds";
    public static final int VIEWER_SET_EXPIRY_SECONDS_DEFAULT = (int) TimeUnit.DAYS.toSeconds(1);

    public static final String DISPLAY_NAME_CACHE_EXPIRY_SECONDS = "whoslooking.display-name-cache-expiry.seconds";
    public static final int DISPLAY_NAME_CACHE_EXPIRY_SECONDS_DEFAULT = (int) TimeUnit.DAYS.toSeconds(1);

    public static final String POLLER_INTERVAL_SECONDS = "whoslooking.poller-interval.seconds";
    public static final int POLLER_INTERVAL_SECONDS_DEFAULT = 10;

    public static final String AVATAR_SIZE_PX = "whoslooking.avatar-size";
    public static final int AVATAR_SIZE_PX_DEFAULT = 24;

    public static final String KEY_SEPARATOR = "#";
    public static final String PER_PAGE_VIEW_TOKEN_HEADER = "X-acpt";
    
    public static final String ANALYTICS_EXPIRY_SECONDS = "whoslooking.analytics-expiry.seconds";
    public static final int ANALYTICS_EXPIRY_SECONDS_DEFAULT = (int) TimeUnit.DAYS.toSeconds(30);

    public static final String VALIDATION_TOKEN_EXPIRY_SECONDS = "whoslooking.validation-token-expiry.seconds";
    public static final int VALIDATION_TOKEN_SECONDS_DEFAULT = (int) TimeUnit.DAYS.toSeconds(2);
    
    public static final String DISPLAY_NAME_FETCH_BLACKLIST_EXPIRY_SECONDS = "whoslooking.display-name-cache-expiry.seconds";
    public static final int DISPLAY_NAME_FETCH_BLACKLIST_EXPIRY_SECONDS_DEFAULT = (int) TimeUnit.DAYS.toSeconds(1);

    // feature flags
    public static final String ENABLE_METRICS = "whoslooking.feature.metrics";
    public static final String ENABLE_DISPLAY_NAME_FETCH = "whoslooking.feature.display-name-fetch";
    public static final String ENABLE_DISPLAY_NAME_FETCH_BLACKLIST = "whoslooking.feature.display-name-fetch-blacklist";
    public static final String ENABLE_ENCRYPTION_UPGRADE_TASK = "whoslooking.feature.enable-encryption_upgrade-task";
    
}
