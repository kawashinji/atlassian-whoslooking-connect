package service;

import org.codehaus.jackson.JsonNode;

import play.Logger;
import play.cache.Cache;
import play.libs.WS;
import play.libs.F.Callback;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS.Response;

import com.atlassian.connect.play.java.AC;

public class ViewerDetailsService {

	public String getDetailsFor(final String hostId, final String username)
	{
		   Object value = Cache.get(hostId + "-" + username + "-details");
	        if (value == null)
	        {
	            Logger.info("Calling...");
	            Promise<Response> promise = AC.url("/rest/api/latest/user")
	            		.setQueryParameter("username", username) .get();

	            promise.onRedeem(new Callback<WS.Response>()
	            {

	                @Override
	                public void invoke(Response a) throws Throwable
	                {
	                    JsonNode asJson = a.asJson();
	                    if (!asJson.has("errorMessages"))
	                    {
	                        Cache.set(hostId + "-" + username + "-details", asJson.toString(), 1200);
	                    }
	                    else
	                    {
	                        Logger.error(asJson.toString());
	                    }
	                }
	            });

	            promise.recover(new Function<Throwable, WS.Response>()
	            {
	                @Override
	                public WS.Response apply(Throwable t)
	                {
	                    Logger.error("An error occurred", t);
	                    // Can't really recover from this, so just rethrow.
	                    throw new RuntimeException(t);
	                }
	            });
	        }
	        
	        return (String) value;
	        
	        
	}
	
}
