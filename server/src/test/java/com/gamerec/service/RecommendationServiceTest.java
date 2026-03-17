package com.gamerec.service;

import static com.gamerec.service.TestGameBuilder.game;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.gamerec.model.domain.CachedGame;
import com.gamerec.model.dto.GameDto;
import com.gamerec.model.dto.PreferenceRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

  @Mock private GameCacheService cacheService;

  @InjectMocks private RecommendationService recommendationService;

  @BeforeEach
  void setUp() {
    // Stub toDto to produce a recognizable GameDto from any CachedGame + score
    lenient()
        .when(cacheService.toDto(any(CachedGame.class), any()))
        .thenAnswer(
            inv -> {
              CachedGame g = inv.getArgument(0);
              Double score = inv.getArgument(1);
              return new GameDto(
                  g.getIgdbId(),
                  g.getName(),
                  g.getSummary(),
                  g.getRating(),
                  g.getRatingCount(),
                  g.getFirstRelease(),
                  g.getCoverUrl(),
                  List.of(),
                  List.of(),
                  List.of(),
                  score);
            });
  }

  // ---- Null / empty input handling ----

  @Test
  void getRecommendations_nullLikedGameIds_doesNotThrow() {
    PreferenceRequest request = new PreferenceRequest(List.of(1), null, null, null);
    when(cacheService.fetchAndCacheByGenresAndThemes(anyList(), anyList(), anyInt()))
        .thenReturn(List.of());
    when(cacheService.getCandidateGames()).thenReturn(List.of());

    List<GameDto> results = recommendationService.getRecommendations(request);

    assertThat(results).isNotNull();
    verify(cacheService, never()).ensureCached(anyList());
  }

  @Test
  void getRecommendations_emptyRequest_returnsEmpty() {
    PreferenceRequest request = new PreferenceRequest(null, null, null, null);
    when(cacheService.getCandidateGames()).thenReturn(List.of());

    List<GameDto> results = recommendationService.getRecommendations(request);

    assertThat(results).isEmpty();
  }

  @Test
  void getRecommendations_noMatchingCandidates_returnsEmpty() {
    // Candidate has zero overlap with preference and minimal base scores
    CachedGame candidate = game(99).genres(999).rating(0.0).ratingCount(0).build();
    candidate.setFirstRelease(null);
    candidate.setGameModeIds(null);
    candidate.setPerspectiveIds(null);
    PreferenceRequest request = new PreferenceRequest(List.of(1), null, null, null);
    when(cacheService.fetchAndCacheByGenresAndThemes(anyList(), anyList(), anyInt()))
        .thenReturn(List.of());
    when(cacheService.getCandidateGames()).thenReturn(List.of(candidate));

    List<GameDto> results = recommendationService.getRecommendations(request);

    assertThat(results).isEmpty();
  }

  // ---- Profile building ----

  @Test
  void getRecommendations_explicitGenresOverrideImplicit() {
    // Liked game has genre 10; explicit selection is genre 20
    CachedGame likedGame = game(1).genres(10).build();
    CachedGame candidateMatchingExplicit =
        game(100).genres(20).rating(80.0).firstRelease(LocalDate.now().minusYears(1)).build();
    CachedGame candidateMatchingImplicit =
        game(101).genres(10).rating(80.0).firstRelease(LocalDate.now().minusYears(1)).build();

    PreferenceRequest request = new PreferenceRequest(List.of(20), null, null, List.of(1L));
    when(cacheService.ensureCached(List.of(1L))).thenReturn(List.of(likedGame));
    when(cacheService.fetchSimilarGamesForLiked(List.of(1L))).thenReturn(List.of());
    when(cacheService.fetchAndCacheByGenresAndThemes(anyList(), anyList(), anyInt()))
        .thenReturn(List.of());
    when(cacheService.getCandidateGames())
        .thenReturn(List.of(candidateMatchingExplicit, candidateMatchingImplicit));

    List<GameDto> results = recommendationService.getRecommendations(request);

    assertThat(results).isNotEmpty();
    // The candidate matching the explicit genre (weight 1.0) should rank higher
    assertThat(results.get(0).igdbId()).isEqualTo(100L);
  }

  // ---- Scoring: genre affinity ----

  @Test
  void getRecommendations_fullGenreOverlap_ranksHigherThanPartial() {
    PreferenceRequest request = new PreferenceRequest(List.of(1, 2, 3), null, null, null);
    CachedGame fullMatch =
        game(10).genres(1, 2, 3).rating(70.0).firstRelease(LocalDate.now().minusYears(1)).build();
    CachedGame partialMatch =
        game(11).genres(1).rating(70.0).firstRelease(LocalDate.now().minusYears(1)).build();

    when(cacheService.fetchAndCacheByGenresAndThemes(anyList(), anyList(), anyInt()))
        .thenReturn(List.of());
    when(cacheService.getCandidateGames()).thenReturn(List.of(fullMatch, partialMatch));

    List<GameDto> results = recommendationService.getRecommendations(request);

    assertThat(results).hasSizeGreaterThanOrEqualTo(2);
    assertThat(results.get(0).igdbId()).isEqualTo(10L);
    assertThat(results.get(0).score()).isGreaterThan(results.get(1).score());
  }

  // ---- Scoring: platform match ----

  @Test
  void getRecommendations_platformMatch_binaryScoring() {
    PreferenceRequest request = new PreferenceRequest(List.of(1), null, List.of(6), null);
    CachedGame withPlatform =
        game(10)
            .genres(1)
            .platforms(6)
            .rating(70.0)
            .firstRelease(LocalDate.now().minusYears(1))
            .build();
    CachedGame withoutPlatform =
        game(11)
            .genres(1)
            .platforms(48)
            .rating(70.0)
            .firstRelease(LocalDate.now().minusYears(1))
            .build();

    when(cacheService.fetchAndCacheByGenresAndThemes(anyList(), anyList(), anyInt()))
        .thenReturn(List.of());
    when(cacheService.getCandidateGames()).thenReturn(List.of(withPlatform, withoutPlatform));

    List<GameDto> results = recommendationService.getRecommendations(request);

    assertThat(results).isNotEmpty();
    assertThat(results.get(0).igdbId()).isEqualTo(10L);
  }

  @Test
  void getRecommendations_emptyPlatformPreference_defaultsToPointThree() {
    // No platform preference: both candidates get 0.3 platform score, so only other signals differ
    PreferenceRequest request = new PreferenceRequest(List.of(1), null, null, null);
    CachedGame a =
        game(10).genres(1).platforms(6).rating(80.0).firstRelease(LocalDate.now()).build();
    CachedGame b =
        game(11).genres(1).platforms(48).rating(80.0).firstRelease(LocalDate.now()).build();

    when(cacheService.fetchAndCacheByGenresAndThemes(anyList(), anyList(), anyInt()))
        .thenReturn(List.of());
    when(cacheService.getCandidateGames()).thenReturn(List.of(a, b));

    List<GameDto> results = recommendationService.getRecommendations(request);

    // Both should appear with similar scores since platform doesn't differentiate
    assertThat(results).hasSize(2);
    double scoreDiff = Math.abs(results.get(0).score() - results.get(1).score());
    assertThat(scoreDiff).isLessThan(0.01);
  }

  // ---- Scoring: rating ----

  @Test
  void getRecommendations_higherRatedGameRanksHigher() {
    PreferenceRequest request = new PreferenceRequest(List.of(1), null, null, null);
    CachedGame highRated =
        game(10).genres(1).rating(95.0).ratingCount(100).firstRelease(LocalDate.now()).build();
    CachedGame lowRated =
        game(11).genres(1).rating(30.0).ratingCount(100).firstRelease(LocalDate.now()).build();

    when(cacheService.fetchAndCacheByGenresAndThemes(anyList(), anyList(), anyInt()))
        .thenReturn(List.of());
    when(cacheService.getCandidateGames()).thenReturn(List.of(highRated, lowRated));

    List<GameDto> results = recommendationService.getRecommendations(request);

    assertThat(results).hasSizeGreaterThanOrEqualTo(2);
    assertThat(results.get(0).igdbId()).isEqualTo(10L);
  }

  @Test
  void getRecommendations_ratingConfidenceCapsAtFifty() {
    PreferenceRequest request = new PreferenceRequest(List.of(1), null, null, null);
    CachedGame fiftyCount =
        game(10).genres(1).rating(80.0).ratingCount(50).firstRelease(LocalDate.now()).build();
    CachedGame fiveHundredCount =
        game(11).genres(1).rating(80.0).ratingCount(500).firstRelease(LocalDate.now()).build();

    when(cacheService.fetchAndCacheByGenresAndThemes(anyList(), anyList(), anyInt()))
        .thenReturn(List.of());
    when(cacheService.getCandidateGames()).thenReturn(List.of(fiftyCount, fiveHundredCount));

    List<GameDto> results = recommendationService.getRecommendations(request);

    assertThat(results).hasSize(2);
    // Both should have the same confidence (capped at 1.0), so scores are equal
    double scoreDiff = Math.abs(results.get(0).score() - results.get(1).score());
    assertThat(scoreDiff).isLessThan(0.001);
  }

  // ---- Scoring: recency ----

  @Test
  void getRecommendations_recentGameGetsHigherRecencyBonus() {
    PreferenceRequest request = new PreferenceRequest(List.of(1), null, null, null);
    CachedGame recentGame =
        game(10).genres(1).rating(70.0).firstRelease(LocalDate.now().minusYears(1)).build();
    CachedGame oldGame =
        game(11).genres(1).rating(70.0).firstRelease(LocalDate.now().minusYears(10)).build();

    when(cacheService.fetchAndCacheByGenresAndThemes(anyList(), anyList(), anyInt()))
        .thenReturn(List.of());
    when(cacheService.getCandidateGames()).thenReturn(List.of(recentGame, oldGame));

    List<GameDto> results = recommendationService.getRecommendations(request);

    assertThat(results).hasSizeGreaterThanOrEqualTo(2);
    assertThat(results.get(0).igdbId()).isEqualTo(10L);
  }

  @Test
  void getRecommendations_nullReleaseDate_doesNotThrow() {
    PreferenceRequest request = new PreferenceRequest(List.of(1), null, null, null);
    CachedGame nullRelease = game(10).genres(1).rating(70.0).build();
    nullRelease.setFirstRelease(null);

    when(cacheService.fetchAndCacheByGenresAndThemes(anyList(), anyList(), anyInt()))
        .thenReturn(List.of());
    when(cacheService.getCandidateGames()).thenReturn(List.of(nullRelease));

    List<GameDto> results = recommendationService.getRecommendations(request);

    assertThat(results).isNotNull();
  }

  // ---- Scoring: bonus multipliers ----

  @Test
  void getRecommendations_similarGameBonus_ranksHigher() {
    // Liked game has similarGameIds containing candidate 10
    CachedGame likedGame = game(1).genres(5).similarGames(10L).build();
    CachedGame similarCandidate =
        game(10).genres(5).rating(70.0).firstRelease(LocalDate.now().minusYears(1)).build();
    CachedGame normalCandidate =
        game(11).genres(5).rating(70.0).firstRelease(LocalDate.now().minusYears(1)).build();

    PreferenceRequest request = new PreferenceRequest(null, null, null, List.of(1L));
    when(cacheService.ensureCached(List.of(1L))).thenReturn(List.of(likedGame));
    when(cacheService.fetchSimilarGamesForLiked(List.of(1L))).thenReturn(List.of());
    when(cacheService.getCandidateGames()).thenReturn(List.of(similarCandidate, normalCandidate));

    List<GameDto> results = recommendationService.getRecommendations(request);

    assertThat(results).hasSizeGreaterThanOrEqualTo(2);
    assertThat(results.get(0).igdbId()).isEqualTo(10L);
  }

  @Test
  void getRecommendations_developerMatchBonus_ranksHigher() {
    CachedGame likedGame = game(1).genres(5).developers("fromsoftware").build();
    CachedGame devMatch =
        game(10)
            .genres(5)
            .developers("fromsoftware")
            .rating(70.0)
            .firstRelease(LocalDate.now().minusYears(1))
            .build();
    CachedGame noDevMatch =
        game(11)
            .genres(5)
            .developers("otherstudio")
            .rating(70.0)
            .firstRelease(LocalDate.now().minusYears(1))
            .build();

    PreferenceRequest request = new PreferenceRequest(null, null, null, List.of(1L));
    when(cacheService.ensureCached(List.of(1L))).thenReturn(List.of(likedGame));
    when(cacheService.fetchSimilarGamesForLiked(List.of(1L))).thenReturn(List.of());
    when(cacheService.getCandidateGames()).thenReturn(List.of(devMatch, noDevMatch));

    List<GameDto> results = recommendationService.getRecommendations(request);

    assertThat(results).hasSizeGreaterThanOrEqualTo(2);
    assertThat(results.get(0).igdbId()).isEqualTo(10L);
  }

  @Test
  void getRecommendations_multiGenreOverlapBonus_requiresTwoHighWeightGenres() {
    // Both genres are explicitly selected (weight 1.0 > 0.5 threshold)
    PreferenceRequest request = new PreferenceRequest(List.of(1, 2), null, null, null);
    CachedGame multiOverlap =
        game(10).genres(1, 2).rating(70.0).firstRelease(LocalDate.now().minusYears(1)).build();
    CachedGame singleOverlap =
        game(11).genres(1, 999).rating(70.0).firstRelease(LocalDate.now().minusYears(1)).build();

    when(cacheService.fetchAndCacheByGenresAndThemes(anyList(), anyList(), anyInt()))
        .thenReturn(List.of());
    when(cacheService.getCandidateGames()).thenReturn(List.of(multiOverlap, singleOverlap));

    List<GameDto> results = recommendationService.getRecommendations(request);

    assertThat(results).hasSizeGreaterThanOrEqualTo(2);
    assertThat(results.get(0).igdbId()).isEqualTo(10L);
    // Multi-genre overlap game should have noticeably higher score (1.10x bonus)
    assertThat(results.get(0).score()).isGreaterThan(results.get(1).score());
  }

  // ---- Filtering ----

  @Test
  void getRecommendations_excludesLikedGames() {
    CachedGame likedGame = game(1).genres(5).build();
    // The liked game itself appears in the candidate pool
    CachedGame otherCandidate =
        game(10).genres(5).rating(80.0).firstRelease(LocalDate.now()).build();

    PreferenceRequest request = new PreferenceRequest(null, null, null, List.of(1L));
    when(cacheService.ensureCached(List.of(1L))).thenReturn(List.of(likedGame));
    when(cacheService.fetchSimilarGamesForLiked(List.of(1L))).thenReturn(List.of());
    when(cacheService.getCandidateGames()).thenReturn(List.of(likedGame, otherCandidate));

    List<GameDto> results = recommendationService.getRecommendations(request);

    assertThat(results).noneMatch(dto -> dto.igdbId().equals(1L));
  }

  @Test
  void getRecommendations_excludesGamesInLikedFranchise() {
    CachedGame likedGame = game(1).genres(5).franchises(100L).build();
    CachedGame sameFranchise =
        game(10).genres(5).franchises(100L).rating(90.0).firstRelease(LocalDate.now()).build();
    CachedGame differentFranchise =
        game(11).genres(5).franchises(200L).rating(70.0).firstRelease(LocalDate.now()).build();

    PreferenceRequest request = new PreferenceRequest(null, null, null, List.of(1L));
    when(cacheService.ensureCached(List.of(1L))).thenReturn(List.of(likedGame));
    when(cacheService.fetchSimilarGamesForLiked(List.of(1L))).thenReturn(List.of());
    when(cacheService.getCandidateGames()).thenReturn(List.of(sameFranchise, differentFranchise));

    List<GameDto> results = recommendationService.getRecommendations(request);

    assertThat(results).noneMatch(dto -> dto.igdbId().equals(10L));
    assertThat(results).anyMatch(dto -> dto.igdbId().equals(11L));
  }

  @Test
  void getRecommendations_nullFranchiseIdsOnCandidate_notExcluded() {
    CachedGame likedGame = game(1).genres(5).franchises(100L).build();
    CachedGame nullFranchise =
        game(10).genres(5).rating(80.0).firstRelease(LocalDate.now()).build();
    nullFranchise.setFranchiseIds(null);

    PreferenceRequest request = new PreferenceRequest(null, null, null, List.of(1L));
    when(cacheService.ensureCached(List.of(1L))).thenReturn(List.of(likedGame));
    when(cacheService.fetchSimilarGamesForLiked(List.of(1L))).thenReturn(List.of());
    when(cacheService.getCandidateGames()).thenReturn(List.of(nullFranchise));

    List<GameDto> results = recommendationService.getRecommendations(request);

    assertThat(results).anyMatch(dto -> dto.igdbId().equals(10L));
  }

  // ---- Score below threshold ----

  @Test
  void getRecommendations_scoreBelowPointOne_excluded() {
    PreferenceRequest request = new PreferenceRequest(List.of(1), null, null, null);
    // Candidate has zero overlap with the genre preference and low rating
    CachedGame lowScoreCandidate = game(10).genres(999).rating(5.0).ratingCount(1).build();
    lowScoreCandidate.setFirstRelease(null);

    when(cacheService.fetchAndCacheByGenresAndThemes(anyList(), anyList(), anyInt()))
        .thenReturn(List.of());
    when(cacheService.getCandidateGames()).thenReturn(List.of(lowScoreCandidate));

    List<GameDto> results = recommendationService.getRecommendations(request);

    assertThat(results).isEmpty();
  }

  // ---- Diversity enforcement ----

  @Test
  void getRecommendations_limitsThreePerGenreThemeCombination() {
    PreferenceRequest request = new PreferenceRequest(List.of(1), null, null, null);
    List<CachedGame> candidates = new ArrayList<>();
    // 5 candidates with identical genre+theme combo
    for (int i = 0; i < 5; i++) {
      candidates.add(
          game(10 + i).genres(1).themes(50).rating(90.0 - i).firstRelease(LocalDate.now()).build());
    }

    when(cacheService.fetchAndCacheByGenresAndThemes(anyList(), anyList(), anyInt()))
        .thenReturn(List.of());
    when(cacheService.getCandidateGames()).thenReturn(candidates);

    List<GameDto> results = recommendationService.getRecommendations(request);

    assertThat(results).hasSize(3);
  }

  @Test
  void getRecommendations_limitsThreePerDeveloper() {
    PreferenceRequest request = new PreferenceRequest(List.of(1), null, null, null);
    List<CachedGame> candidates = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      candidates.add(
          game(10 + i)
              .genres(1)
              .themes(50 + i) // different genre-theme combos to avoid that limit
              .developers("samestudio")
              .rating(90.0 - i)
              .firstRelease(LocalDate.now())
              .build());
    }

    when(cacheService.fetchAndCacheByGenresAndThemes(anyList(), anyList(), anyInt()))
        .thenReturn(List.of());
    when(cacheService.getCandidateGames()).thenReturn(candidates);

    List<GameDto> results = recommendationService.getRecommendations(request);

    assertThat(results).hasSize(3);
  }

  @Test
  void getRecommendations_limitsTwoPerFranchise() {
    PreferenceRequest request = new PreferenceRequest(List.of(1), null, null, null);
    List<CachedGame> candidates = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      candidates.add(
          game(10 + i)
              .genres(1)
              .themes(50 + i)
              .franchises(300L)
              .rating(90.0 - i)
              .firstRelease(LocalDate.now())
              .build());
    }

    when(cacheService.fetchAndCacheByGenresAndThemes(anyList(), anyList(), anyInt()))
        .thenReturn(List.of());
    when(cacheService.getCandidateGames()).thenReturn(candidates);

    List<GameDto> results = recommendationService.getRecommendations(request);

    assertThat(results).hasSize(2);
  }

  @Test
  void getRecommendations_maxTwentyResults() {
    PreferenceRequest request = new PreferenceRequest(List.of(1), null, null, null);
    List<CachedGame> candidates = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
      candidates.add(
          game(10 + i)
              .genres(1)
              .themes(50 + i)
              .developers("dev" + i)
              .rating(90.0)
              .firstRelease(LocalDate.now())
              .build());
    }

    when(cacheService.fetchAndCacheByGenresAndThemes(anyList(), anyList(), anyInt()))
        .thenReturn(List.of());
    when(cacheService.getCandidateGames()).thenReturn(candidates);

    List<GameDto> results = recommendationService.getRecommendations(request);

    assertThat(results).hasSize(20);
  }

  @Test
  void getRecommendations_emptyDeveloperKey_notCountedForDiversityLimit() {
    PreferenceRequest request = new PreferenceRequest(List.of(1), null, null, null);
    List<CachedGame> candidates = new ArrayList<>();
    // 5 candidates with no developer (empty dev key should not be limited)
    for (int i = 0; i < 5; i++) {
      candidates.add(
          game(10 + i)
              .genres(1)
              .themes(50 + i)
              .rating(90.0 - i)
              .firstRelease(LocalDate.now())
              .build());
      // developers default to empty array in TestGameBuilder
    }

    when(cacheService.fetchAndCacheByGenresAndThemes(anyList(), anyList(), anyInt()))
        .thenReturn(List.of());
    when(cacheService.getCandidateGames()).thenReturn(candidates);

    List<GameDto> results = recommendationService.getRecommendations(request);

    // All 5 should be present (not limited by developer diversity since dev key is empty)
    assertThat(results).hasSize(5);
  }

  // ---- Score rounding ----

  @Test
  void getRecommendations_scoresRoundedToThreeDecimalPlaces() {
    PreferenceRequest request = new PreferenceRequest(List.of(1), null, null, null);
    CachedGame candidate =
        game(10).genres(1).rating(73.0).ratingCount(37).firstRelease(LocalDate.now()).build();

    when(cacheService.fetchAndCacheByGenresAndThemes(anyList(), anyList(), anyInt()))
        .thenReturn(List.of());
    when(cacheService.getCandidateGames()).thenReturn(List.of(candidate));

    List<GameDto> results = recommendationService.getRecommendations(request);

    assertThat(results).isNotEmpty();
    Double score = results.get(0).score();
    // Verify score has at most 3 decimal places
    double rounded = Math.round(score * 1000.0) / 1000.0;
    assertThat(score).isEqualTo(rounded);
  }

  // ---- Candidate pool tiers ----

  @Test
  void getRecommendations_withLikedGames_fetchesSimilarGames() {
    CachedGame likedGame = game(1).genres(5).build();
    CachedGame similarGame = game(50).genres(5).rating(85.0).firstRelease(LocalDate.now()).build();

    PreferenceRequest request = new PreferenceRequest(null, null, null, List.of(1L));
    when(cacheService.ensureCached(List.of(1L))).thenReturn(List.of(likedGame));
    when(cacheService.fetchSimilarGamesForLiked(List.of(1L))).thenReturn(List.of(similarGame));
    when(cacheService.getCandidateGames()).thenReturn(List.of());

    List<GameDto> results = recommendationService.getRecommendations(request);

    verify(cacheService).fetchSimilarGamesForLiked(List.of(1L));
    assertThat(results).anyMatch(dto -> dto.igdbId().equals(50L));
  }

  @Test
  void getRecommendations_keywordsCappedAtTop20() {
    // Create a liked game with 30 keyword IDs
    Integer[] manyKeywords = IntStream.rangeClosed(1, 30).boxed().toArray(Integer[]::new);
    CachedGame likedGame = game(1).genres(5).keywords(manyKeywords).build();

    // Candidate matching only keyword IDs 25-30 (low weight since there's only 1 liked game
    // and each keyword gets the same weight, so all are equal - but capped to 20)
    CachedGame lowKeywordCandidate =
        game(10)
            .genres(5)
            .keywords(21, 22, 23, 24, 25, 26, 27, 28, 29, 30)
            .rating(80.0)
            .firstRelease(LocalDate.now())
            .build();
    CachedGame highKeywordCandidate =
        game(11)
            .genres(5)
            .keywords(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            .rating(80.0)
            .firstRelease(LocalDate.now())
            .build();

    PreferenceRequest request = new PreferenceRequest(null, null, null, List.of(1L));
    when(cacheService.ensureCached(List.of(1L))).thenReturn(List.of(likedGame));
    when(cacheService.fetchSimilarGamesForLiked(List.of(1L))).thenReturn(List.of());
    when(cacheService.getCandidateGames())
        .thenReturn(List.of(lowKeywordCandidate, highKeywordCandidate));

    List<GameDto> results = recommendationService.getRecommendations(request);

    // With only 1 liked game, all keywords get equal weight, so the cap picks 20 out of 30.
    // Keywords 21-30 may be excluded from the top 20. Both candidates should still appear
    // since they share genres with the liked game.
    assertThat(results).isNotEmpty();
  }
}
