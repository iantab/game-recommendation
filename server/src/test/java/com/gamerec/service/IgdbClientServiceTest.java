package com.gamerec.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.gamerec.config.IgdbConfig;
import com.gamerec.model.igdb.IgdbGame;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class IgdbClientServiceTest {

  @Mock private IgdbTokenManager tokenManager;
  @Mock private ObjectMapper objectMapper;
  @Mock private RestClient restClient;
  @Mock private RestClient.RequestBodyUriSpec requestBodyUriSpec;
  @Mock private RestClient.RequestBodySpec requestBodySpec;
  @Mock private RestClient.ResponseSpec responseSpec;

  private IgdbClientService igdbClientService;
  private final ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

  @BeforeEach
  void setUp() {
    IgdbConfig config =
        new IgdbConfig(
            "test-client-id",
            "test-secret",
            "https://api.igdb.com/v4",
            "https://id.twitch.tv/oauth2/token");
    igdbClientService = new IgdbClientService(config, tokenManager, objectMapper);
    // Replace the internally-created RestClient with our mock
    ReflectionTestUtils.setField(igdbClientService, "restClient", restClient);
  }

  private void stubRestClientChain(String jsonResponse) {
    when(restClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
    when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
    when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.body(String.class)).thenReturn(jsonResponse);
    lenient().when(tokenManager.getAccessToken()).thenReturn("test-token");
  }

  // ---- Query sanitization ----

  @Test
  void searchGames_sanitizesDoubleQuotes() throws Exception {
    stubRestClientChain("[]");
    when(objectMapper.readValue(eq("[]"), any(TypeReference.class))).thenReturn(List.of());

    igdbClientService.searchGames("game \"test\"", 10);

    verify(requestBodySpec).body(bodyCaptor.capture());
    assertThat(bodyCaptor.getValue()).contains("game \\\"test\\\"");
  }

  @Test
  void searchGames_sanitizesSemicolons() throws Exception {
    stubRestClientChain("[]");
    when(objectMapper.readValue(eq("[]"), any(TypeReference.class))).thenReturn(List.of());

    igdbClientService.searchGames("game;drop", 10);

    verify(requestBodySpec).body(bodyCaptor.capture());
    assertThat(bodyCaptor.getValue()).contains("gamedrop");
    assertThat(bodyCaptor.getValue()).doesNotContain("game;drop");
  }

  // ---- Limit clamping ----

  @Test
  void searchGames_limitZero_defaultsToTwenty() throws Exception {
    stubRestClientChain("[]");
    when(objectMapper.readValue(eq("[]"), any(TypeReference.class))).thenReturn(List.of());

    igdbClientService.searchGames("test", 0);

    verify(requestBodySpec).body(bodyCaptor.capture());
    assertThat(bodyCaptor.getValue()).contains("limit 20;");
  }

  @Test
  void searchGames_limitNegative_defaultsToTwenty() throws Exception {
    stubRestClientChain("[]");
    when(objectMapper.readValue(eq("[]"), any(TypeReference.class))).thenReturn(List.of());

    igdbClientService.searchGames("test", -5);

    verify(requestBodySpec).body(bodyCaptor.capture());
    assertThat(bodyCaptor.getValue()).contains("limit 20;");
  }

  @Test
  void searchGames_limitOverFifty_clampedToFifty() throws Exception {
    stubRestClientChain("[]");
    when(objectMapper.readValue(eq("[]"), any(TypeReference.class))).thenReturn(List.of());

    igdbClientService.searchGames("test", 100);

    verify(requestBodySpec).body(bodyCaptor.capture());
    assertThat(bodyCaptor.getValue()).contains("limit 50;");
  }

  @Test
  void searchGames_limitWithinRange_unchanged() throws Exception {
    stubRestClientChain("[]");
    when(objectMapper.readValue(eq("[]"), any(TypeReference.class))).thenReturn(List.of());

    igdbClientService.searchGames("test", 25);

    verify(requestBodySpec).body(bodyCaptor.capture());
    assertThat(bodyCaptor.getValue()).contains("limit 25;");
  }

  // ---- getGame ----

  @Test
  void getGame_emptyResponse_returnsNull() throws Exception {
    stubRestClientChain("[]");
    when(objectMapper.readValue(eq("[]"), any(TypeReference.class))).thenReturn(List.of());

    IgdbGame result = igdbClientService.getGame(123);

    assertThat(result).isNull();
  }

  @Test
  void getGame_singleResult_returnsFirst() throws Exception {
    IgdbGame game =
        new IgdbGame(
            123L, "Test", null, null, null, null, null, null, null, null, null, null, null, null,
            null, null);
    stubRestClientChain("[{}]");
    when(objectMapper.readValue(eq("[{}]"), any(TypeReference.class))).thenReturn(List.of(game));

    IgdbGame result = igdbClientService.getGame(123);

    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(123L);
  }

  // ---- getGamesByIds ----

  @Test
  void getGamesByIds_nullInput_returnsEmpty() {
    List<IgdbGame> result = igdbClientService.getGamesByIds(null);
    assertThat(result).isEmpty();
  }

  @Test
  void getGamesByIds_emptyInput_returnsEmpty() {
    List<IgdbGame> result = igdbClientService.getGamesByIds(List.of());
    assertThat(result).isEmpty();
  }

  @Test
  void getGamesByIds_formatsIdListCorrectly() throws Exception {
    stubRestClientChain("[]");
    when(objectMapper.readValue(eq("[]"), any(TypeReference.class))).thenReturn(List.of());

    igdbClientService.getGamesByIds(List.of(1L, 2L, 3L));

    verify(requestBodySpec).body(bodyCaptor.capture());
    assertThat(bodyCaptor.getValue()).contains("where id = (1,2,3);");
  }

  // ---- getGamesByGenresAndThemes ----

  @Test
  void getGamesByGenresAndThemes_bothNull_returnsEmpty() {
    List<IgdbGame> result = igdbClientService.getGamesByGenresAndThemes(null, null, 10);
    assertThat(result).isEmpty();
  }

  @Test
  void getGamesByGenresAndThemes_genresOnly_hasGenresClause() throws Exception {
    stubRestClientChain("[]");
    when(objectMapper.readValue(eq("[]"), any(TypeReference.class))).thenReturn(List.of());

    igdbClientService.getGamesByGenresAndThemes(List.of(12, 14), null, 10);

    verify(requestBodySpec).body(bodyCaptor.capture());
    String body = bodyCaptor.getValue();
    assertThat(body).contains("genres = (12,14)");
    assertThat(body).doesNotContain("themes =");
  }

  @Test
  void getGamesByGenresAndThemes_bothPresent_combinesWithAmpersand() throws Exception {
    stubRestClientChain("[]");
    when(objectMapper.readValue(eq("[]"), any(TypeReference.class))).thenReturn(List.of());

    igdbClientService.getGamesByGenresAndThemes(List.of(12), List.of(17), 10);

    verify(requestBodySpec).body(bodyCaptor.capture());
    String body = bodyCaptor.getValue();
    assertThat(body).contains("genres = (12)");
    assertThat(body).contains("& themes = (17)");
  }
}
