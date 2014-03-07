package com.atlassian.connect.play.java.oauth;

import com.atlassian.connect.play.java.BaseUrl;
import com.atlassian.fugue.Option;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import play.mvc.Http;

import java.util.Map;

import static com.atlassian.fugue.Option.option;

public final class PlayRequestHelper implements RequestHelper<Http.Request>
{
    public String getHttpMethod(Http.Request request)
    {
        return request.method();
    }

    public String getUrl(Http.Request request, BaseUrl baseUrl)
    {
        return baseUrl.get() + request.path();
    }

    public Multimap<String, String> getParameters(Http.Request request)
    {
        final ImmutableMultimap.Builder<String, String> map = ImmutableMultimap.builder();
        for (Map.Entry<String, String[]> entry : request.queryString().entrySet())
        {
            for (String v : entry.getValue())
            {
                final String k = entry.getKey();
                map.put(k, v);
            }
        }
        return map.build();
    }

    @Override
    public Option<String> getHeader(Http.Request request, String name)
    {
        return option(request.getHeader(name));
    }
}
