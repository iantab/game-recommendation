package com.gamerec.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.gamerec.config.IgdbConfig;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class IgdbTokenManagerTest {

  @Mock private RestClient restClient;
  @Mock private RestClient.RequestBodyUriSpec requestBodyUriSpec;
  @Mock private RestClient.RequestBodySpec requestBodySpec;
  @Mock private RestClient.ResponseSpec responseSpec;

  private IgdbTokenManager tokenManager;

  @BeforeEach
  void setUp() {
    IgdbConfig config =
        new IgdbConfig(
            "client-id",
            "client-secret",
            "https://api.igdb.com/v4",
            "https://id.twitch.tv/oauth2/token");
    tokenManager = new IgdbTokenManager(config);
    ReflectionTestUtils.setField(tokenManager, "restClient", restClient);
  }

  private void stubTokenResponse(String token, long expiresIn) {
    when(restClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
    when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenReturn(responseSpec);

    IgdbTokenManager.TokenResponse tokenResponse =
        new IgdbTokenManager.TokenResponse(token, expiresIn);
    when(responseSpec.body(IgdbTokenManager.TokenResponse.class)).thenReturn(tokenResponse);
  }

  @Test
  void getAccessToken_firstCall_refreshesToken() {
    stubTokenResponse("fresh-token", 3600);

    String token = tokenManager.getAccessToken();

    assertThat(token).isEqualTo("fresh-token");
    verify(restClient).post();
  }

  @Test
  void getAccessToken_tokenNotExpired_returnsCached() {
    stubTokenResponse("cached-token", 3600);

    String first = tokenManager.getAccessToken();
    String second = tokenManager.getAccessToken();

    assertThat(first).isEqualTo("cached-token");
    assertThat(second).isEqualTo("cached-token");
    // Only one HTTP call should have been made
    verify(restClient, times(1)).post();
  }

  @Test
  void getAccessToken_tokenExpired_refreshes() {
    stubTokenResponse("first-token", 3600);
    tokenManager.getAccessToken();

    // Force expiry
    ReflectionTestUtils.setField(tokenManager, "expiresAt", Instant.EPOCH);

    stubTokenResponse("second-token", 3600);
    String token = tokenManager.getAccessToken();

    assertThat(token).isEqualTo("second-token");
    verify(restClient, times(2)).post();
  }

  @Test
  void getAccessToken_expirySetWithBufferOf300Seconds() {
    stubTokenResponse("token", 3600);
    Instant before = Instant.now();

    tokenManager.getAccessToken();

    Instant expiresAt = (Instant) ReflectionTestUtils.getField(tokenManager, "expiresAt");
    // expiresAt should be approximately now + 3300 seconds (3600 - 300 buffer)
    assertThat(expiresAt).isAfter(before.plusSeconds(3200));
    assertThat(expiresAt).isBefore(before.plusSeconds(3400));
  }
}
