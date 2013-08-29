package service;

import java.util.Map;


public interface HeartbeatService
{

    /**
     * @return map of userids actively viewing <code>resourceId</code> on <code>hostId</code>, with the UTC timestamp of their last heartbeat.
     */
    Map<String, String> list(final String hostId, final String resourceId);

    /**
     * Record the fact that userId has recently viewed resourceId on hostId. It is up to the implementation to expire
     * this event as appropriate.
     */
    void put(final String hostId, final String resourceId, final String userId);

    /**
     * Record the fact that userId has stopped viewing resourceId on hostId.
     * <p>
     * There is no guarantee this will be called when the user stops viewing the resource. The implementation is
     * responsible for expiring viewers as appropriate.
     */
    void delete(final String hostId, final String resourceId, final String viewer);

}
