package com.gamerec.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gamerec.config.IgdbConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Service
public class IgdbTokenManager {

    private static final Logger log = LoggerFactory.getLogger(IgdbTokenManager.class);
    private final IgdbConfig config;
    private final RestClient restClient;

    private String accessToken;
    private Instant expiresAt = Instant.EPOCH;

    public IgdbTokenManager(IgdbConfig config) {
        this.config = config;
        this.restClient = RestClient.create();
    }

    public synchronized String getAccessToken() {
        if (accessToken == null || Instant.now().isAfter(expiresAt)) {
            refreshToken();
        }
        return accessToken;
    }

    private void refreshToken() {
        log.info("Requesting new IGDB/Twitch access token");
        String body = "client_id=" + config.clientId()
                + "&client_secret=" + config.clientSecret()
                + "&grant_type=client_credentials";

        TokenResponse response = restClient.post()
                .uri(config.tokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(TokenResponse.class);

        this.accessToken = response.accessToken();
        this.expiresAt = Instant.now().plusSeconds(response.expiresIn() - 300);
        log.info("IGDB token acquired, expires at {}", expiresAt);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn
    ) {
    }
}
