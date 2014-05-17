package service;

import java.util.Map;

import com.atlassian.fugue.Effect;
import com.atlassian.fugue.Option;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import org.javasimon.Sample;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.UnknownSample;

import play.Play;

import static utils.Constants.ENABLE_METRICS;

public class MetricsService
{

    public Option<Split> start(String key)
    {
        return isEnabled() ? Option.some(SimonManager.getStopwatch(key).start()) : Option.<Split> none();
    }

    public void stop(Option<Split> split)
    {
        split.foreach(new Effect<Split>()
        {
            @Override
            public void apply(Split s)
            {
                s.stop();
            }
        });
    }

    public <T> T withMetric(String key, Supplier<T> s)
    {
        Option<Split> timer = start(key);
        try
        {
            return s.get();
        }
        finally
        {
            stop(timer);
        }
    }

    private boolean isEnabled()
    {
        return Play.application().configuration().getBoolean(ENABLE_METRICS, true);
    }

    public long incCounter(String key)
    {
        
        return isEnabled() ? SimonManager.getCounter(key).increase().getCounter() : 0;
    }

    public Map<String, Sample> getAllSamples()
    {
        Builder<String, Sample> samples = ImmutableMap.<String, Sample>builder();
        for (String key: SimonManager.getSimonNames())
        {
            Sample sample = SimonManager.getSimon(key).sample();
            if (! (sample instanceof UnknownSample))
            {
                samples.put(key, sample);
            }
        }
        return samples.build();
    }
}
