package com.atlassian.connect.play.java.oauth;

import com.atlassian.connect.play.java.BaseUrl;
import com.atlassian.fugue.Option;
import com.google.common.collect.Multimap;

interface RequestHelper<R>
{
    String getHttpMethod(R request);

    String getUrl(R request, BaseUrl baseUrl);

    Multimap<String, String> getParameters(R request);

    Option<String> getHeader(R request, String name);
}
