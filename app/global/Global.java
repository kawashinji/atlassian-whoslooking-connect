package global;

import com.atlassian.connect.play.java.AC;
import com.atlassian.connect.play.java.AcHost;
import com.atlassian.connect.play.java.play.AcGlobalSettings;

import com.atlassian.connect.play.java.service.AcHostRepository;
import com.atlassian.connect.play.java.service.AcHostService;
import com.atlassian.connect.play.java.service.InjectorFactory;
import com.google.common.collect.ObjectArrays;
import play.Application;
import play.Logger;
import play.Play;
import play.api.mvc.EssentialFilter;
import play.filters.gzip.GzipFilter;
import filters.AddResponseHeader;
import utils.VersionUtils;

import static utils.Constants.ENABLE_ENCRYPTION_UPGRADE_TASK;

public final class Global extends AcGlobalSettings
{
    @Override
    public void onStart(Application app)
    {
        Logger.info(prettyName() + " has started.");

        if (Play.application().configuration().getBoolean(ENABLE_ENCRYPTION_UPGRADE_TASK)) {
            runEncryptionUpgradeTask();
        }
    }

    private void runEncryptionUpgradeTask() {
        Logger.info("Running encryption upgrade task...");
        try {
            AcHostService acHostService = InjectorFactory.getAcHostService();
            for (AcHost host : acHostService.all()) {
                try {
                    String encryptedSharedSecret = host.getEncryptedSharedSecret();
                    if (encryptedSharedSecret == null) {
                        if (host.getSharedSecret() != null) {
                            Logger.info("Persisting " + host.getBaseUrl() + " with encrypted shared secret.");
                            acHostService.registerHost(host);
                        } else {
                            Logger.warn("Skipping " + host.getBaseUrl() + ": no shared secret to encrypt");
                        }
                    } else {
                        Logger.info("Skipping " + host.getBaseUrl() + ": already has encrypted shared secret.");
                    }
                } catch (Throwable t) {
                    Logger.error("Failed encryption upgrade task on host " + host.getBaseUrl(), t);
                }
            }
        } catch (Throwable t) {
            Logger.error("Failed encryption upgrade task", t);
        }
    }

    @Override
    public void onStop(Application app)
    {
        Logger.info(prettyName() + " has stopped.");
    }
    
    private String prettyName()
    {
        // This can't be a static final, else it gets invoked before the application is initalialised,
        // and therefore the component values can't be read from the config.
        return String.format("%s (%s) version %s", AC.PLUGIN_NAME, AC.PLUGIN_KEY, VersionUtils.VERSION);
    }

    public <T extends EssentialFilter> Class<T>[] filters() {
        return (Class[])((Class[]) ObjectArrays.concat(AddResponseHeader.class, super.filters()));
    }

}
