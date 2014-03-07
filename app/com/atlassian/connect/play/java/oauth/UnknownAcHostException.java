package com.atlassian.connect.play.java.oauth;

import static com.google.common.base.Preconditions.checkNotNull;

public final class UnknownAcHostException extends RuntimeException
{
    private final String consumerKey;

    public UnknownAcHostException(String consumerKey)
    {
        this.consumerKey = checkNotNull(consumerKey);
    }

    public String getConsumerKey()
    {
        return consumerKey;
    }
}
