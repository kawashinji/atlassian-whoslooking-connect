package util;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import utils.Constants;

import java.util.UUID;

/**
 * Container for heartbeat identifier params with separator character in fields.
 */
public class FakeHeartbeat {
    public final String hostId;
    public final String resourceId;
    public final String userId;

    public FakeHeartbeat(String hostId, String resourceId, String userId) {
        this.hostId = hostId;
        this.resourceId = resourceId;
        this.userId = userId;
    }

    public static FakeHeartbeat build()
    {
      return new FakeHeartbeat("host" + Constants.KEY_SEPARATOR + UUID.randomUUID().toString(),
              "resource" + Constants.KEY_SEPARATOR + UUID.randomUUID().toString(),
              "user" + Constants.KEY_SEPARATOR + UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
