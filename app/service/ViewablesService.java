package service;

import java.util.Collection;
import java.util.Map;

import org.codehaus.jackson.JsonNode;

/**
 *
 */
public interface ViewablesService
{

    /**
     * @return set of userids actively viewing <code>resourceId</code> on <code>hostId</code>
     */
    Collection<String> getViewers(final String hostId, final String resourceId);

    /**
     * Record the fact that userId has recently viewed resourceId on hostId. It is up to the implementation to expire
     * this event as appropriate.
     */
    void putViewer(final String hostId, final String resourceId, final String userId);

    /**
     * Record the fact that userId has stopped viewing resourceId on hostId.
     * <p>
     * There is no guarantee this will be called when the user stops viewing the resource. The implementation is
     * responsible for expiring viewers as appropriate.
     */
    void deleteViewer(final String hostId, final String resourceId, final String viewer);

    /**
     * @return map of userids actively viewing <code>resourceId</code> on <code>hostId</code> to any additional details
     *         we have about the user.
     */
    Map<String, JsonNode> getViewersWithDetails(final String resourceId, final String hostId);

}
