package com.atlassian.connect.play.java.oauth;

import com.atlassian.connect.play.java.AC;

import com.google.common.base.Supplier;
import com.google.common.io.Files;

import org.h2.store.fs.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static com.atlassian.connect.play.java.util.Utils.LOGGER;
import static com.google.common.base.Suppliers.memoize;
import static java.lang.String.format;

public class OAuthKeys
{
    public static final String OAUTH_LOCAL_PUBLIC_KEY = "OAUTH_LOCAL_PUBLIC_KEY";
    public static final String OAUTH_LOCAL_PUBLIC_KEY_FILE = "OAUTH_LOCAL_PUBLIC_KEY_FILE";
    public static final String OAUTH_LOCAL_PRIVATE_KEY = "OAUTH_LOCAL_PRIVATE_KEY";
    public static final String OAUTH_LOCAL_PRIVATE_KEY_FILE = "OAUTH_LOCAL_PRIVATE_KEY_FILE";

    public static final String PUBLIC_KEY_PEM = "public-key.pem";
    public static final String PRIVATE_KEY_PEM = "private-key.pem";

    public static final Supplier<String> publicKey = memoize(new Supplier<String>()
    {
        @Override
        public String get()
        {
            return getKey(OAUTH_LOCAL_PUBLIC_KEY, OAUTH_LOCAL_PUBLIC_KEY_FILE, PUBLIC_KEY_PEM);
        }
    });

    public static final Supplier<String> privateKey = memoize(new Supplier<String>()
    {
        @Override
        public String get()
        {
            return getKey(OAUTH_LOCAL_PRIVATE_KEY, OAUTH_LOCAL_PRIVATE_KEY_FILE, PRIVATE_KEY_PEM);
        }
    });

    private static String getKey(String envKey, String envFilePath, String defaultFilename)
    {
        //first try to read the key from a file if this environment var was provided.
        final String keyFilePath = getOptionalEnv(envFilePath, null);
        if (keyFilePath != null && FileUtils.exists(keyFilePath))
        {
            try
            {
                final String key = getFileContent(keyFilePath);
                if (AC.isDev())
                {
                    LOGGER.debug(format("Loaded key '%s' from file '%s' as:\n%s", envKey, keyFilePath, key));
                }
                return key;
            }
            catch (IOException e)
            {
                LOGGER.warn(format("Could not read key file '%s'.", keyFilePath), e);
            }
        }

        //next try to get the key directly from an environment property.
        final String key = getOptionalEnv(envKey, null);
        if (key != null)
        {
            if (AC.isDev())
            {
                LOGGER.debug(format("Loaded key '%s' as:\n%s", envKey, key));
            }
            return key;
        }

        //finally if we're in dev mode fall back to the default file that should have been auto-generated
        if (AC.isDev())
        {
            try
            {
                return getFileContent(defaultFilename);
            }
            catch (IOException e)
            {
                LOGGER.warn(format("Could not read '%s' file.", defaultFilename), e);
            }
        }

        throw new IllegalStateException(format("Could NOT find valid file path environment variable %s or environment variable %s for OAuth key!", envKey, envFilePath));
    }

    private static String getFileContent(String pathname) throws IOException
    {
        final StringBuilder sb = new StringBuilder();
        Files.copy(new File(pathname), Charset.forName("UTF-8"), sb);
        return sb.toString();
    }

    public static String getOptionalEnv(String name, String def)
    {
        final String val = System.getenv().get(name);
        if (val == null)
        {
            return def;
        }
        else
        {
            return val.replaceAll("\\\\n", "\n");
        }
    }

}
