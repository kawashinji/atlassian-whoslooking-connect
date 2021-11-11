package service;

import java.net.URL;
import java.security.SignatureException;


import com.atlassian.connect.play.java.AC;
import com.atlassian.connect.play.java.auth.jwt.*;
import com.atlassian.connect.play.java.controllers.AcController;

import com.atlassian.jwt.CanonicalHttpRequest;
import com.atlassian.jwt.core.HttpRequestCanonicalizer;
import com.atlassian.jwt.core.SimpleJwt;
import com.atlassian.jwt.core.reader.NimbusJwtReaderFactory;
import com.google.common.base.Supplier;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import net.minidev.json.JSONObject;

import com.atlassian.jwt.CanonicalHttpRequest;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

public class ApiMigrationsService {
    public boolean validateQsh(Http.Request request, String jwtString) {
        try {
            PlayRequestWrapper wrappedReq = new PlayRequestWrapper(request, (new URL((String) AC.baseUrl.get())).getPath());
            CanonicalHttpRequest cannonicalRequest = wrappedReq.getCanonicalHttpRequest();
            Logger.info("Cannonical request: " + HttpRequestCanonicalizer.canonicalize(cannonicalRequest));

            JWSObject jwso = JWSObject.parse(jwtString);
            JSONObject payload = jwso.getPayload().toJSONObject();
            if (payload.get("qsh") == null) {
                Logger.error("qsh missing from payload");
                return false;
            }

            String qsh = payload.get("qsh").toString();
            Logger.info("Input qsh value: " + qsh);
            String computedHash = HttpRequestCanonicalizer.computeCanonicalRequestHash(cannonicalRequest);
            Logger.info("Computed qsh value: " + computedHash);

            if (!computedHash.equals(qsh)) {
                Logger.error("qsh check failure: [computed: " + computedHash + "]; [qsh: " + qsh +"]");
                return false;
            }

        } catch (Exception e) {
            Logger.error("Failed to parse install JWT token", e);
            return false;
        }

        return true;
    }

    public static void validSignedInstall() throws SignatureException {

    }
}
