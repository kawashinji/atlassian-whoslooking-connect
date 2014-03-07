package com.atlassian.connect.play.java.oauth;

import com.atlassian.connect.play.java.oauth.OAuthRequestValidatorAction;
import play.mvc.With;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@With(OAuthRequestValidatorAction.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckValidOAuthRequest
{
}

