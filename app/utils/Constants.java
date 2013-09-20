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
    public static final String PER_PAGE_VIEW_TOKEN_HEADER = "x-per-page-view-token";

}
