package util;

import java.util.concurrent.Callable;

public class TimedAsserts {


    public static final int DEFAULT_RETRY_INTERVAL_MS = 100;
    public static final int DEFAULT_ERROR_MARGIN_MS = 200;

    public static void assertStartsPassingAfter(long timeToWaitMs, Callable<Void> callable) throws Exception
    {
        TimedAsserts.assertStartsPassingAfter(timeToWaitMs, callable, DEFAULT_RETRY_INTERVAL_MS, DEFAULT_ERROR_MARGIN_MS);
    }

    public static void assertStartsPassingAfter(long timeToWaitMs, Callable<Void> callable, int retryIntervalMs, int errorMarginMs) throws Exception
    {
        long startTime = System.currentTimeMillis();
        while (true)
        {
            long waitedMs = System.currentTimeMillis() - startTime;
            try
            {
                callable.call();
                if (waitedMs < (timeToWaitMs-errorMarginMs))
                {
                    throw new AssertionError("Started passing too soon: " + waitedMs);
                }
                return;
            }
            catch (AssertionError e)
            {
                if (waitedMs > timeToWaitMs+errorMarginMs)
                {
                    throw e;
                }
            }
            Thread.sleep(retryIntervalMs);
        }
    }
}
