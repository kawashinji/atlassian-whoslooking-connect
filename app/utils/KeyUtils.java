package utils;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringUtils;

/**
 * Provides helper methods to encode/decode the keys used to store values in Redis
 */
public class KeyUtils
{
    // TODO: replace with PercentEscaper when guava 15 is available.
    static URLCodec codec = new URLCodec("UTF-8");

    public static String buildHeartbeatKey(final String hostId, final String resourceId, final String accountId)
    {
        return buildKey("heartbeat", hostId, resourceId, accountId);
    }

    public static String extractAccountIdFromHeartbeatKey(final String key)
    {
        String[] components = key.split(Constants.KEY_SEPARATOR);
        String encodedAccountId = components[components.length-1];
        try {
            return codec.decode(encodedAccountId);
        } catch (DecoderException e) {
            throw new RuntimeException(e);
        }
    }

    public static String buildViewerSetKey(final String hostId, final String resourceId)
    {
        return buildKey("viewersetv2", hostId, resourceId);
    }
    
    public static String buildUserKey(final String hostId, final String accountId)
    {
        return buildKey(hostId, accountId);
    }

    public static String buildDisplayNameKey(final String hostId, final String accountId)
    {
        return buildKey("cache", hostId, accountId, "displayName");
    }

    private static String buildKey(final String... components)
    {
       String[] encodedComponents = new String[components.length];
       for (int i=0; i<components.length; ++i)
       {
           try {
               encodedComponents[i] = codec.encode(components[i]);
           } catch (EncoderException e) {
               throw new RuntimeException(e);
           }
       }

       return StringUtils.join(encodedComponents, Constants.KEY_SEPARATOR);
    }

}
