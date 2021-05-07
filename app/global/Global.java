package global;

import com.atlassian.connect.play.java.AC;
import com.atlassian.connect.play.java.play.AcGlobalSettings;

import com.google.common.collect.ObjectArrays;
import play.Application;
import play.Logger;
import play.api.mvc.EssentialFilter;
import play.filters.gzip.GzipFilter;
import filters.AddResponseHeader;
import utils.VersionUtils;

public final class Global extends AcGlobalSettings
{
    @Override
    public void onStart(Application app)
    {
        Logger.info(prettyName() + " has started.");
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
