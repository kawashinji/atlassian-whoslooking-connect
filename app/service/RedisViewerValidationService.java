package service;

import play.Logger;
import play.Play;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.Base64;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static utils.Constants.VALIDATION_TOKEN_EXPIRY_SECONDS;
import static utils.Constants.VALIDATION_TOKEN_SECONDS_DEFAULT;
import static utils.KeyUtils.buildHeartbeatKey;
import static utils.KeyUtils.buildVerificationKey;
import static utils.RedisUtils.jedisPool;

public class RedisViewerValidationService implements ViewerValidationService {

    private final int userValidationTokenExpiry;

    public RedisViewerValidationService()
    {
        this.userValidationTokenExpiry = Play.application().configuration().getInt(VALIDATION_TOKEN_EXPIRY_SECONDS, VALIDATION_TOKEN_SECONDS_DEFAULT);
    }

    @Override
    public String setToken(String hostId, String resourceId, String accountId) {
        final String key = buildVerificationKey(hostId, resourceId, accountId);
        final String token = buildRandomToken();

        Logger.debug(format("Setting token %s/%s/%s (key:%s value:%s)", hostId, resourceId, accountId, key, token));

        Jedis j = jedisPool().getResource();
        try
        {
            Transaction t = j.multi();
            t.set(key, token);
            t.expire(key, userValidationTokenExpiry);
            t.exec();
        }
        finally
        {
            jedisPool().returnResource(j);
        }
        return token;
    }

    @Override
    public boolean verifyToken(String hostId, String resourceId, String accountId, String token) {
        Jedis j = jedisPool().getResource();
        final String key = buildVerificationKey(hostId, resourceId, accountId);
        boolean isValid = false;
        try
        {
            String value = j.get(key);
            if (Objects.nonNull(token)) {
                isValid = token.equals(value);
            }
            Logger.debug(format("Retrieved token %s/%s/%s (key:%s value:%s); compared to %s as %s.",
                    hostId, resourceId, accountId, key, value, token, isValid));
        }
        finally
        {
            jedisPool().returnResource(j);
        }
        return isValid;
    }

    private String buildRandomToken() {
        Random random = ThreadLocalRandom.current();
        byte[] randomBytes = new byte[32];
        random.nextBytes(randomBytes);
        String encoded = Base64.getUrlEncoder().encodeToString(randomBytes);
        return encoded;
    }
}
