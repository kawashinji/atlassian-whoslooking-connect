package utils;

import com.atlassian.connect.play.java.AC;

import org.apache.commons.lang3.StringUtils;

import play.Play;
import play.libs.Json;

public class VersionUtils
{
    private static String ATLASSIAN_DEPLOYMENT_VERSIONS = Play.application().configuration().getString("atlassian.deployment.versions");

    public static String VERSION;

    static
    {
        // Fail fast on startup if there are any issues parsing the version. This is invoked via Global.onStart.
        VERSION = Json.parse(ATLASSIAN_DEPLOYMENT_VERSIONS).get(AC.PLUGIN_KEY).asText();
        if (StringUtils.isEmpty(VERSION))
        {
            throw new IllegalStateException("Invalid version: " + VERSION);
        }
    }

}
