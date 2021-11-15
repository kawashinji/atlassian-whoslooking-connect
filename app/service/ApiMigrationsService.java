package service;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;

import com.atlassian.connect.play.java.AC;
import com.atlassian.connect.play.java.auth.jwt.*;

import com.atlassian.jwt.CanonicalHttpRequest;
import com.atlassian.jwt.core.HttpRequestCanonicalizer;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import net.minidev.json.JSONObject;

import play.Logger;
import play.mvc.Http;
import utils.JWTUtils;

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

    public static boolean validateSignedInstall(Http.Request request, String jwtString) {

        // validate the baseURL
        if (AC.baseUrl.get() == null || AC.baseUrl.get().equals("")) {
            Logger.error("BaseURL not found");
            return false;
        }

        // validate the claims
        try {
            validateClaims(jwtString, AC.baseUrl.get());
        } catch(Exception e) {
           Logger.error("Claims validation failed", e);
           return false;
        }

        try {
            validateAsymmetricSignature(jwtString);
        } catch(Exception e) {
            Logger.error("Could not verify asymmetric signature", e);
            return false;
        }

        Logger.info("Signed Install Verification Successful");
        return true;
    }

    private static void validateClaims(String jwtString, String baseURL) throws Exception {
        JWSObject jwso = JWSObject.parse(jwtString);
        JSONObject claims = jwso.getPayload().toJSONObject();

        if (!claims.containsKey("iss")) {
            Logger.error("Could not parse issuer.");
            throw new Exception("Could not parse issuer.");
        }

        if (!claims.containsKey("exp") || Integer.parseInt(claims.get("exp").toString()) <= System.currentTimeMillis() / 1000) {
            Logger.error("JWT Expired");
            throw new Exception("JWT Expired");
        }

        // matching audience with baseURL for claim validation
        Logger.info("Actual AUD: " + claims.get("aud"));
        Logger.info("Expected AUD: " + baseURL);
        if (!claims.get("aud").toString().equals(baseURL)) {
            Logger.error("JWT claim does not match with expected audience");
            throw new Exception("JWT claim does not match with expected audience");
        }
    }

    private static void validateAsymmetricSignature(String jwtString) throws Exception {
        JWSObject jwso = JWSObject.parse(jwtString);
        RSAPublicKey rsaPublicKey = JWTUtils.fetchRSAPublicKey(jwso.getHeader().getKeyID());
        byte[] signingInput = jwso.getSigningInput();

        RSASSAVerifier verifier = new RSASSAVerifier(rsaPublicKey);

        if (!verifier.verify(jwso.getHeader(), signingInput, jwso.getSignature())) {
            Logger.error("Signature Verification Failed");
            throw new Exception("Signature Verification Failed");
        }
    }


}
