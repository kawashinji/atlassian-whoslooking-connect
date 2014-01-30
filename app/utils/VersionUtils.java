package utils;

import com.atlassian.connect.play.java.AC;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;

import play.Logger;
import play.Play;
import play.libs.Json;

public class VersionUtils
{
    private static String ATLASSIAN_DEPLOYMENT_VERSIONS = Play.application().configuration().getString("atlassian.deployment.versions");

    public static String VERSION;

    static
    {
        // Fail fast on startup if there are any issues parsing the version. This is invoked via Global.onStart.
        Logger.info("Starting with addon key " + AC.PLUGIN_KEY + " and version string " + ATLASSIAN_DEPLOYMENT_VERSIONS);
        JsonNode versionJson = Json.parse(ATLASSIAN_DEPLOYMENT_VERSIONS);
        if (versionJson == null) {
            throw new IllegalStateException("Failed to extract version from version string: " + ATLASSIAN_DEPLOYMENT_VERSIONS);
        }
        VERSION = versionJson.get(AC.PLUGIN_KEY).asText();
        if (StringUtils.isEmpty(VERSION))
        {
            throw new IllegalStateException("Invalid version: " + VERSION);
        }
    }

}
