package util;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.IdentityHashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import play.test.FakeApplication;
import redis.embedded.RedisServer;

import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.start;
import static play.test.Helpers.stop;

public class RedisTestUtils
{
    /**
     * Track Redis servers so we can stop them when the corresponding fake app is stopped.
     */
    static Map<FakeApplication, LocalRedisServer> fakeAppToRedis = new IdentityHashMap<>();

    public static FakeApplication startNewFakeAppWithRedis() throws Exception
    {
        return startNewFakeAppWithRedis(ImmutableMap.<String,String>of());
    }
    
    public static FakeApplication startNewFakeAppWithRedis(Map<String, String> extraConfig) throws Exception
    {
        LocalRedisServer redisServer = new LocalRedisServer();
        redisServer.start();

        Map<String, String> fakeConfig = ImmutableMap.<String, String> builder()
            .put("redis.uri", redisServer.getUri())
            .putAll(extraConfig)
            .build();
        
        FakeApplication fakeApp = fakeApplication(fakeConfig);
        fakeAppToRedis.put(fakeApp, redisServer);
        
        start(fakeApp);
        return fakeApp;
        
    }
    
    public static void stopFakeAppWithRedis(FakeApplication fakeApp)
    {
        stop(fakeApp);
        fakeAppToRedis.get(fakeApp).stop();
        fakeAppToRedis.remove(fakeApp);
    }

    public static class LocalRedisServer
    {
        private final int port;
        private final RedisServer delegate;

        public LocalRedisServer() throws Exception
        {
            port = findFreePort();
            delegate = new RedisServer(port);
        }

        public void start() throws IOException
        {
            delegate.start();
        }

        public void stop()
        {
            delegate.stop();
        }

        public int getPort()
        {
            return port;
        }

        public String getUri()
        {
            return "redis://localhost:" + port;
        }
    }

    private static int findFreePort() throws IOException
    {
        ServerSocket s = new ServerSocket(0);
        int port = s.getLocalPort();
        s.close();
        return port;
    }
}
