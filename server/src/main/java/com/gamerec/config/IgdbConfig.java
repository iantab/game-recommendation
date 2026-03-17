package com.gamerec.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "igdb")
public record IgdbConfig(String clientId, String clientSecret, String baseUrl, String tokenUrl) {
}
