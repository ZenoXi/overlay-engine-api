package com.zenox.overlayenginewebapi.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenox.overlayenginewebapi.model.response.osuweb.Response;
import com.zenox.overlayenginewebapi.model.response.osuweb.Token;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

@Service
@CommonsLog
public class TokenService {
    @Value("${osuweb.client-id}")
    private String clientId;
    @Value("${osuweb.client-secret}")
    private String clientSecret;
    @Value("${osuweb.endpoint-url}")
    private String endpointUrl;

    @Getter
    @AllArgsConstructor
    private static class TokenData {
        private String secret;
        private Instant authenticationTime;
        private Long lifetime;
    }
    private TokenData token;

    public synchronized String tryGetToken() {
        long tokenDeathBuffer = 60;
        if (token != null && Instant.now().isAfter(token.getAuthenticationTime().plusSeconds(token.getLifetime()).minusSeconds(tokenDeathBuffer))) {
            token = null;
        }

        if (token == null) {
            Response<Token> tokenResponse = authenticate();
            if (tokenResponse.getContent() != null) {
                token = new TokenData(tokenResponse.getContent().getSecret(), Instant.now(), tokenResponse.getContent().getExpiresIn());
            } else {
                return null;
            }
        }

        return token.getSecret();
    }

    private Response<Token> authenticate() {
        String body = "client_id=" + clientId + "&client_secret=" + clientSecret + "&grant_type=client_credentials&scope=public";
        HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl + "/oauth/token"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .build();

        try {
            HttpResponse<String> tokenResponse = HttpClient.newHttpClient().send(tokenRequest, HttpResponse.BodyHandlers.ofString());
            if (tokenResponse.statusCode() != HttpStatus.OK.value()) {
                log.error("Authentication failed, response body: " + tokenResponse.body());
                return new Response<>(HttpStatus.resolve(tokenResponse.statusCode()), "", null);
            }

            ObjectMapper mapper = new ObjectMapper();
            return new Response<>(mapper.readValue(tokenResponse.body(), Token.class));

        } catch (IOException | InterruptedException e) {
            log.error("Exception during authentication", e);
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR, e.getLocalizedMessage(), null);
        }
    }
}
