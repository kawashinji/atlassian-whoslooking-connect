package service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.atlassian.fugue.Effect;
import com.atlassian.fugue.Option;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import org.apache.commons.lang3.StringUtils;
import org.javasimon.Sample;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.UnknownSample;

import play.Logger;
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
                sendToHostedGraphite(s.getStopwatch().getName(), TimeUnit.NANOSECONDS.toMillis(s.runningFor()));
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
        sendToHostedGraphite(key, 1);
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
    
    public void sendToHostedGraphite(String key, long value) {
        String apikey = System.getenv("HOSTEDGRAPHITE_APIKEY");
        if (StringUtils.isNotEmpty(apikey)) {
            try (DatagramSocket sock   = new DatagramSocket()) {
                InetAddress addr      = InetAddress.getByName("carbon.hostedgraphite.com");
                byte[] message        = (apikey + "." + key + " " + value + "\n").getBytes();
                DatagramPacket packet = new DatagramPacket(message, message.length, addr, 2003);
                sock.send(packet);
            }
            catch (Exception e)
            {
                Logger.error("Failed to send metric.", e);
            }

        }
    }
}
