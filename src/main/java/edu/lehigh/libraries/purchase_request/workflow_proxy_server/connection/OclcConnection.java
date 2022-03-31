package edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection;

import java.time.Instant;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OclcConnection {

    public static final String WORLDCAT_BASE_URL = "https://americas.discovery.api.oclc.org/worldcat/search/v2";
    private static final long TOKEN_BUFFER_SECONDS = 30;

    private final Config config;
    private OAuth2AccessToken token;
    private long tokenExpiration;
    private OAuth20Service oclcService;

    public OclcConnection(Config config, String scope) throws Exception {
        this.config = config;

        initConnection(scope);

        log.debug("OCLC service ready");
    }

    private void initConnection(String scope) {
        String clientId = config.getOclc().getWsKey();
        String clientSecret = config.getOclc().getSecret();
        oclcService = new ServiceBuilder(clientId)
            .apiSecret(clientSecret)
            .defaultScope(scope)
            .build(OclcApi.instance());
        getToken();
    }

    private void getToken() {
        try {
            token = oclcService.getAccessTokenClientCredentialsGrant();
        }
        catch (Exception e) {
            log.error("Error connecting to OCLC: ", e);
            return;
        }
        tokenExpiration = Instant.now().getEpochSecond() + token.getExpiresIn().intValue();
        log.debug("Connected to OCLC");
        log.debug("Response was: " + token.getRawResponse());
    }

    private void checkConnection() {
        long timeLeft = tokenExpiration - Instant.now().getEpochSecond();
        log.debug("Token expires in " + timeLeft + " seconds.");
        if (timeLeft - TOKEN_BUFFER_SECONDS <= 0) {
            log.debug("Renewing token.");
            getToken();
        } 
    }

    public JsonObject execute(String url) throws Exception {
        checkConnection();

        OAuthRequest request = new OAuthRequest(Verb.GET, url);
        request.addHeader("Accept", "application/json");
        oclcService.signRequest(token, request);
        Response response;
        String responseBody;
        response = oclcService.execute(request);
        log.debug("got bib response from oclc:" + response);
        responseBody = response.getBody();

        JsonObject responseObject = JsonParser.parseString(responseBody).getAsJsonObject();
        return responseObject;
    }

    private static class OclcApi extends DefaultApi20 {

        private static final String TOKEN_URL = "https://oauth.oclc.org/token";

        private static final OclcApi INSTANCE = new OclcApi();
        public static OclcApi instance() {
            return INSTANCE;
        }

        @Override
        public String getAccessTokenEndpoint() {
            return TOKEN_URL;
        }

        @Override
        protected String getAuthorizationBaseUrl() {
            throw new UnsupportedOperationException("No BaseURL needed for Client Credentials API.");
        }

    }

}
