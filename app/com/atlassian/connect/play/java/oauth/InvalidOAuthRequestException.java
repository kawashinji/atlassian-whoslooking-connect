package com.atlassian.connect.play.java.oauth;

public final class InvalidOAuthRequestException extends RuntimeException
{
    public InvalidOAuthRequestException(String msg)
    {
        super(msg);
    }
}