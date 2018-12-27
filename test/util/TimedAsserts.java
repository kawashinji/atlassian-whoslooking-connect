package util;

import java.util.concurrent.Callable;

public class TimedAsserts {

    public static final long DEFAULT_RETRY_INTERVAL_MS = 100;
    public static final long DEFAULT_TIMEOUT_MS = 200;

    public static void assertStartsPassingAfter(long timeToWaitMs, Callable<Void> callable) throws Exception
    {
        TimedAsserts.assertStartsPassingAfter(timeToWaitMs, callable, DEFAULT_TIMEOUT_MS, DEFAULT_RETRY_INTERVAL_MS, true);
    }
    
    public static void assertStartsPassingBefore(long timeOut, Callable<Void> callable) throws Exception
    {
        TimedAsserts.assertStartsPassingAfter(0L, callable, timeOut, DEFAULT_RETRY_INTERVAL_MS, true);
    }

    public static void assertPassesContinuouslyFor(long duration, Callable<Void> callable) throws Exception
    {
        TimedAsserts.assertStartsPassingAfter(0L, callable, duration, DEFAULT_RETRY_INTERVAL_MS, false);
    }

    public static void assertStartsPassingAfter(long timeToWaitMs, Callable<Void> callable, long timeOutMs, long retryIntervalMs, boolean failIfPassTooEarly) throws Exception
    {
        long startTime = System.currentTimeMillis();
        while (true)
        {
            long waitedMs = System.currentTimeMillis() - startTime;
            try
            {
                callable.call();
                if (failIfPassTooEarly && waitedMs < timeToWaitMs)
                {
                    throw new AssertionError("Started passing too soon: " + waitedMs);
                }
                return;
            }
            catch (AssertionError e)
            {
                if (waitedMs > timeToWaitMs+timeOutMs)
                {
                    throw e;
                }
            }
            Thread.sleep(retryIntervalMs);
        }
    }

}
