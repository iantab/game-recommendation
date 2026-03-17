package com.gamerec.service;

import static com.gamerec.service.TestGameBuilder.game;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.gamerec.model.domain.*;
import com.gamerec.model.dto.GameDto;
import com.gamerec.model.dto.GenreDto;
import com.gamerec.model.dto.PlatformDto;
import com.gamerec.model.dto.ThemeDto;
import com.gamerec.model.igdb.IgdbGame;
import com.gamerec.model.igdb.IgdbRef;
import com.gamerec.repository.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class GameCacheServiceTest {

  @Mock private IgdbClientService igdbClient;
  @Mock private CachedGameRepository gameRepo;
  @Mock private GenreRepository genreRepo;
  @Mock private ThemeRepository themeRepo;
  @Mock private PlatformRepository platformRepo;
  @Mock private GameModeRepository gameModeRepo;
  @Mock private PlayerPerspectiveRepository perspectiveRepo;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private GameCacheService cacheService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(cacheService, "ttlDays", 7);
  }

  // ---- searchGames ----

  @Test
  void searchGames_delegatesToIgdbAndReturnsResults() {
    IgdbGame igdbGame =
        new IgdbGame(
            1L,
            "Test Game",
            "Summary",
            80.0,
            100,
            null,
            null,
            List.of(new IgdbRef(1, "RPG", "rpg", null)),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    when(igdbClient.searchGames("test", 10)).thenReturn(List.of(igdbGame));

    List<GameDto> results = cacheService.searchGames("test", 10);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).name()).isEqualTo("Test Game");
    verify(gameRepo).save(any(CachedGame.class));
  }

  @Test
  void searchGames_emptyResults_returnsEmptyList() {
    when(igdbClient.searchGames("nonexistent", 10)).thenReturn(List.of());

    List<GameDto> results = cacheService.searchGames("nonexistent", 10);

    assertThat(results).isEmpty();
    verify(gameRepo, never()).save(any());
  }

  // ---- getGame ----

  @Test
  void getGame_cacheHitNotExpired_returnsFromCache() {
    CachedGame cached = game(1).name("Cached Game").build();
    when(gameRepo.findById(1L)).thenReturn(Optional.of(cached));

    GameDto result = cacheService.getGame(1);

    assertThat(result).isNotNull();
    assertThat(result.name()).isEqualTo("Cached Game");
    verify(igdbClient, never()).getGame(anyLong());
  }

  @Test
  void getGame_cacheHitButExpired_fetchesFromIgdb() {
    CachedGame expired = game(1).cachedAt(LocalDateTime.now().minusDays(10)).build();
    when(gameRepo.findById(1L)).thenReturn(Optional.of(expired));

    IgdbGame fresh =
        new IgdbGame(
            1L,
            "Fresh Game",
            "Summary",
            85.0,
            50,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    when(igdbClient.getGame(1)).thenReturn(fresh);

    GameDto result = cacheService.getGame(1);

    assertThat(result).isNotNull();
    assertThat(result.name()).isEqualTo("Fresh Game");
    verify(igdbClient).getGame(1);
    verify(gameRepo).save(any(CachedGame.class));
  }

  @Test
  void getGame_cacheMiss_fetchesFromIgdb() {
    when(gameRepo.findById(1L)).thenReturn(Optional.empty());
    IgdbGame igdbGame =
        new IgdbGame(
            1L,
            "New Game",
            "Summary",
            75.0,
            30,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    when(igdbClient.getGame(1)).thenReturn(igdbGame);

    GameDto result = cacheService.getGame(1);

    assertThat(result).isNotNull();
    assertThat(result.name()).isEqualTo("New Game");
  }

  @Test
  void getGame_igdbReturnsNull_returnsNull() {
    when(gameRepo.findById(999L)).thenReturn(Optional.empty());
    when(igdbClient.getGame(999)).thenReturn(null);

    GameDto result = cacheService.getGame(999);

    assertThat(result).isNull();
  }

  // ---- ensureCached ----

  @Test
  void ensureCached_nullInput_returnsEmpty() {
    List<CachedGame> result = cacheService.ensureCached(null);
    assertThat(result).isEmpty();
    verify(igdbClient, never()).getGamesByIds(anyList());
  }

  @Test
  void ensureCached_allCached_noIgdbCall() {
    CachedGame cached = game(1).build();
    when(gameRepo.findById(1L)).thenReturn(Optional.of(cached));

    cacheService.ensureCached(List.of(1L));

    verify(igdbClient, never()).getGamesByIds(anyList());
  }

  @Test
  void ensureCached_mixedCachedAndUncached_fetchesOnlyUncached() {
    CachedGame cached = game(1).build();
    when(gameRepo.findById(1L)).thenReturn(Optional.of(cached));
    when(gameRepo.findById(2L)).thenReturn(Optional.empty());

    IgdbGame fetched =
        new IgdbGame(
            2L, "Fetched", null, null, null, null, null, null, null, null, null, null, null, null,
            null, null);
    when(igdbClient.getGamesByIds(List.of(2L))).thenReturn(List.of(fetched));

    cacheService.ensureCached(List.of(1L, 2L));

    verify(igdbClient).getGamesByIds(List.of(2L));
  }

  // ---- fetchSimilarGamesForLiked ----

  @Test
  void fetchSimilarGamesForLiked_nullInput_returnsEmpty() {
    assertThat(cacheService.fetchSimilarGamesForLiked(null)).isEmpty();
  }

  @Test
  void fetchSimilarGamesForLiked_emptyInput_returnsEmpty() {
    assertThat(cacheService.fetchSimilarGamesForLiked(List.of())).isEmpty();
  }

  @Test
  void fetchSimilarGamesForLiked_excludesLikedIdsFromSimilarSet() {
    // Liked game 1 has similar games [2, 3], but 2 is also in the liked list
    CachedGame liked1 = game(1).similarGames(2L, 3L).build();
    CachedGame liked2 = game(2).similarGames().build();
    CachedGame similar3 = game(3).build();

    // First pass: check if liked games are cached
    when(gameRepo.findById(1L)).thenReturn(Optional.of(liked1));
    when(gameRepo.findById(2L)).thenReturn(Optional.of(liked2));
    when(gameRepo.findById(3L)).thenReturn(Optional.of(similar3));

    List<CachedGame> result = cacheService.fetchSimilarGamesForLiked(List.of(1L, 2L));

    // Only game 3 should be returned (game 2 is in liked list)
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getIgdbId()).isEqualTo(3L);
  }

  // ---- loadReferenceData ----

  @Test
  void loadReferenceData_emptyRepos_loadsAll() {
    when(genreRepo.count()).thenReturn(0L);
    when(themeRepo.count()).thenReturn(0L);
    when(platformRepo.count()).thenReturn(0L);
    when(gameModeRepo.count()).thenReturn(0L);
    when(perspectiveRepo.count()).thenReturn(0L);

    when(igdbClient.getGenres()).thenReturn(List.of(new IgdbRef(1, "Action", "action", null)));
    when(igdbClient.getThemes()).thenReturn(List.of(new IgdbRef(1, "Fantasy", "fantasy", null)));
    when(igdbClient.getPlatforms()).thenReturn(List.of(new IgdbRef(6, "PC", "pc", 1)));
    when(igdbClient.getGameModes())
        .thenReturn(List.of(new IgdbRef(1, "Single player", "single-player", null)));
    when(igdbClient.getPlayerPerspectives())
        .thenReturn(List.of(new IgdbRef(1, "First person", "first-person", null)));

    cacheService.loadReferenceData();

    verify(genreRepo).save(any(Genre.class));
    verify(themeRepo).save(any(Theme.class));
    verify(platformRepo).save(any(Platform.class));
    verify(gameModeRepo).save(any(GameMode.class));
    verify(perspectiveRepo).save(any(PlayerPerspective.class));
  }

  @Test
  void loadReferenceData_populatedRepos_skipsAll() {
    when(genreRepo.count()).thenReturn(10L);
    when(themeRepo.count()).thenReturn(10L);
    when(platformRepo.count()).thenReturn(10L);
    when(gameModeRepo.count()).thenReturn(10L);
    when(perspectiveRepo.count()).thenReturn(10L);

    cacheService.loadReferenceData();

    verify(igdbClient, never()).getGenres();
    verify(igdbClient, never()).getThemes();
    verify(igdbClient, never()).getPlatforms();
    verify(igdbClient, never()).getGameModes();
    verify(igdbClient, never()).getPlayerPerspectives();
  }

  // ---- toDto ----

  @Test
  void toDto_mapsAllFieldsCorrectly() {
    CachedGame cached = game(1).name("Elden Ring").rating(96.0).ratingCount(500).build();
    cached.setGenreIds(new Integer[] {12});
    cached.setThemeIds(new Integer[] {17});
    cached.setPlatformIds(new Integer[] {6});

    Genre genre = new Genre(12, "RPG", "rpg");
    Theme theme = new Theme(17, "Fantasy", "fantasy");
    Platform platform = new Platform(6, "PC", "pc", 1);

    when(genreRepo.findAllById(List.of(12))).thenReturn(List.of(genre));
    when(themeRepo.findAllById(List.of(17))).thenReturn(List.of(theme));
    when(platformRepo.findAllById(List.of(6))).thenReturn(List.of(platform));

    GameDto dto = cacheService.toDto(cached, 0.85);

    assertThat(dto.igdbId()).isEqualTo(1L);
    assertThat(dto.name()).isEqualTo("Elden Ring");
    assertThat(dto.rating()).isEqualTo(96.0);
    assertThat(dto.score()).isEqualTo(0.85);
    assertThat(dto.genres()).hasSize(1);
    assertThat(dto.genres().get(0)).isEqualTo(new GenreDto(12, "RPG", "rpg"));
    assertThat(dto.themes()).hasSize(1);
    assertThat(dto.themes().get(0)).isEqualTo(new ThemeDto(17, "Fantasy", "fantasy"));
    assertThat(dto.platforms()).hasSize(1);
    assertThat(dto.platforms().get(0)).isEqualTo(new PlatformDto(6, "PC", "pc", 1));
  }

  @Test
  void toDto_nullArrayFields_returnsEmptyLists() {
    CachedGame cached = game(1).build();
    cached.setGenreIds(null);
    cached.setThemeIds(null);
    cached.setPlatformIds(null);

    GameDto dto = cacheService.toDto(cached, null);

    assertThat(dto.genres()).isEmpty();
    assertThat(dto.themes()).isEmpty();
    assertThat(dto.platforms()).isEmpty();
    assertThat(dto.score()).isNull();
  }

  // ---- getAllGenres / getAllThemes / getAllPlatforms ----

  @Test
  void getAllGenres_returnsGenreDtos() {
    when(genreRepo.findAll()).thenReturn(List.of(new Genre(1, "Action", "action")));

    List<GenreDto> result = cacheService.getAllGenres();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("Action");
  }

  @Test
  void getAllThemes_returnsThemeDtos() {
    when(themeRepo.findAll()).thenReturn(List.of(new Theme(1, "Sci-fi", "sci-fi")));

    List<ThemeDto> result = cacheService.getAllThemes();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("Sci-fi");
  }

  @Test
  void getAllPlatforms_returnsPlatformDtos() {
    when(platformRepo.findAll()).thenReturn(List.of(new Platform(6, "PC", "pc", 1)));

    List<PlatformDto> result = cacheService.getAllPlatforms();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("PC");
  }
}
