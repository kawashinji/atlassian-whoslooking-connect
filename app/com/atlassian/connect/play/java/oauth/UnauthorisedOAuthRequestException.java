package com.atlassian.connect.play.java.oauth;

public final class UnauthorisedOAuthRequestException extends RuntimeException
{
    public UnauthorisedOAuthRequestException(String msg, Throwable throwable)
    {
        super(msg, throwable);
    }
}
