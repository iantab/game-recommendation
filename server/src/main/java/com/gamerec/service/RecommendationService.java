package com.gamerec.service;

import com.gamerec.model.domain.CachedGame;
import com.gamerec.model.dto.GameDto;
import com.gamerec.model.dto.PreferenceRequest;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

  private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

  private final GameCacheService cacheService;

  public RecommendationService(GameCacheService cacheService) {
    this.cacheService = cacheService;
  }

  public List<GameDto> getRecommendations(PreferenceRequest request) {
    List<Long> likedGameIds = request.likedGameIds() != null ? request.likedGameIds() : List.of();

    // 1. Build preference profile from liked games + explicit selections
    PreferenceProfile profile = buildProfile(request, likedGameIds);

    // 2. Build candidate pool (3 tiers)
    Collection<CachedGame> candidatePool = buildCandidatePool(request, profile, likedGameIds);

    // 3. Score all candidates
    List<ScoredGame> scored =
        candidatePool.stream()
            .filter(g -> !profile.likedGameIds.contains(g.getIgdbId()))
            .filter(g -> !isInLikedFranchise(g, profile))
            .map(g -> new ScoredGame(g, scoreGame(g, profile)))
            .filter(sg -> sg.score > 0.1)
            .sorted(Comparator.comparingDouble(ScoredGame::score).reversed())
            .toList();

    // 4. Diversity pass
    List<ScoredGame> diverse = diversify(scored);

    // 5. Convert to DTOs
    return diverse.stream()
        .map(sg -> cacheService.toDto(sg.game, Math.round(sg.score * 1000.0) / 1000.0))
        .toList();
  }

  // ---- Profile Building ----

  private PreferenceProfile buildProfile(PreferenceRequest request, List<Long> likedGameIds) {
    Map<Integer, Double> genreWeights = new HashMap<>();
    Map<Integer, Double> themeWeights = new HashMap<>();
    Map<Integer, Double> keywordWeights = new HashMap<>();
    Set<Integer> gameModeIds = new HashSet<>();
    Set<Integer> perspectiveIds = new HashSet<>();
    Set<Integer> platformIds = toSet(request.platformIds());
    Set<String> developerSlugs = new HashSet<>();
    Set<Long> franchiseIds = new HashSet<>();
    Set<Long> similarGameIds = new HashSet<>();
    Set<Long> likedIds = new HashSet<>(likedGameIds);

    if (!likedGameIds.isEmpty()) {
      List<CachedGame> likedGames = cacheService.ensureCached(likedGameIds);
      int count = likedGames.size();

      for (CachedGame game : likedGames) {
        // Frequency-weighted genre extraction
        addWeights(genreWeights, game.getGenreIds(), count, 0.7);
        addWeights(themeWeights, game.getThemeIds(), count, 0.7);
        addWeights(keywordWeights, game.getKeywordIds(), count, 0.7);

        // Union sets
        addAll(gameModeIds, game.getGameModeIds());
        addAll(perspectiveIds, game.getPerspectiveIds());
        if (game.getDeveloperSlugs() != null) {
          Collections.addAll(developerSlugs, game.getDeveloperSlugs());
        }
        if (game.getFranchiseIds() != null) {
          for (Long fid : game.getFranchiseIds()) franchiseIds.add(fid);
        }
        if (game.getSimilarGameIds() != null) {
          for (Long sid : game.getSimilarGameIds()) similarGameIds.add(sid);
        }
      }
    }

    // Explicit selections override with weight 1.0
    if (request.genreIds() != null) {
      for (Integer id : request.genreIds()) {
        genreWeights.put(id, 1.0);
      }
    }
    if (request.themeIds() != null) {
      for (Integer id : request.themeIds()) {
        themeWeights.put(id, 1.0);
      }
    }

    // Cap keyword weights to top 20 by weight
    if (keywordWeights.size() > 20) {
      Map<Integer, Double> top20 =
          keywordWeights.entrySet().stream()
              .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
              .limit(20)
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      keywordWeights = top20;
    }

    return new PreferenceProfile(
        genreWeights,
        themeWeights,
        keywordWeights,
        gameModeIds,
        perspectiveIds,
        platformIds,
        developerSlugs,
        franchiseIds,
        similarGameIds,
        likedIds);
  }

  private void addWeights(
      Map<Integer, Double> weights, Integer[] ids, int totalGames, double implicitMultiplier) {
    if (ids == null) return;
    double increment = implicitMultiplier / totalGames;
    for (Integer id : ids) {
      weights.merge(id, increment, Double::sum);
    }
  }

  private void addAll(Set<Integer> set, Integer[] ids) {
    if (ids != null) Collections.addAll(set, ids);
  }

  // ---- Candidate Pool ----

  private Collection<CachedGame> buildCandidatePool(
      PreferenceRequest request, PreferenceProfile profile, List<Long> likedGameIds) {
    Map<Long, CachedGame> pool = new LinkedHashMap<>();

    // Tier 1: Similar games graph
    if (!likedGameIds.isEmpty()) {
      List<CachedGame> similarGames = cacheService.fetchSimilarGamesForLiked(likedGameIds);
      similarGames.forEach(g -> pool.putIfAbsent(g.getIgdbId(), g));
      log.info("Tier 1 (similar games): {} candidates", similarGames.size());
    }

    // Tier 2: Attribute-based IGDB fetch
    List<Integer> topGenres = getTopWeightedIds(profile.genreWeights, 3);
    List<Integer> topThemes = getTopWeightedIds(profile.themeWeights, 3);
    if (!topGenres.isEmpty() || !topThemes.isEmpty()) {
      List<CachedGame> attrGames =
          cacheService.fetchAndCacheByGenresAndThemes(topGenres, topThemes, 50);
      attrGames.forEach(g -> pool.putIfAbsent(g.getIgdbId(), g));
      log.info("Tier 2 (attribute fetch): {} candidates", attrGames.size());
    } else if (request.genreIds() != null && !request.genreIds().isEmpty()) {
      // Fallback to genre-only fetch
      List<CachedGame> genreGames = cacheService.fetchAndCacheByGenres(request.genreIds(), 50);
      genreGames.forEach(g -> pool.putIfAbsent(g.getIgdbId(), g));
      log.info("Tier 2 (genre fallback): {} candidates", genreGames.size());
    }

    // Tier 3: Cache pool
    List<CachedGame> cached = cacheService.getCandidateGames();
    cached.forEach(g -> pool.putIfAbsent(g.getIgdbId(), g));
    log.info("Tier 3 (cache): {} candidates, total pool: {}", cached.size(), pool.size());

    return pool.values();
  }

  private List<Integer> getTopWeightedIds(Map<Integer, Double> weights, int limit) {
    return weights.entrySet().stream()
        .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
        .limit(limit)
        .map(Map.Entry::getKey)
        .toList();
  }

  // ---- Scoring ----

  private double scoreGame(CachedGame game, PreferenceProfile profile) {
    Set<Integer> gameGenres = arrayToSet(game.getGenreIds());
    Set<Integer> gameThemes = arrayToSet(game.getThemeIds());
    Set<Integer> gameKeywords = arrayToSet(game.getKeywordIds());
    Set<Integer> gameGameModes = arrayToSet(game.getGameModeIds());
    Set<Integer> gamePerspectives = arrayToSet(game.getPerspectiveIds());
    Set<Integer> gamePlatforms = arrayToSet(game.getPlatformIds());

    // Base signals (sum to 1.0)
    double genreAffinity = weightedJaccard(profile.genreWeights, gameGenres);
    double themeAffinity = weightedJaccard(profile.themeWeights, gameThemes);
    double keywordAffinity = weightedJaccard(profile.keywordWeights, gameKeywords);
    double gameModeCompat = jaccard(profile.gameModeIds, gameGameModes);
    double perspectiveMatch = jaccard(profile.perspectiveIds, gamePerspectives);
    double platformMatch =
        profile.platformIds.isEmpty()
            ? 0.3
            : (hasOverlap(profile.platformIds, gamePlatforms) ? 1.0 : 0.0);
    double normalizedRating = game.getRating() != null ? game.getRating() / 100.0 : 0.0;
    double ratingConfidence =
        game.getRatingCount() != null ? Math.min(1.0, game.getRatingCount() / 50.0) : 0.0;
    double recencyBonus = recencyBonus(game.getFirstRelease());

    double score =
        (0.20 * genreAffinity)
            + (0.15 * themeAffinity)
            + (0.20 * keywordAffinity)
            + (0.08 * gameModeCompat)
            + (0.07 * perspectiveMatch)
            + (0.10 * platformMatch)
            + (0.12 * normalizedRating)
            + (0.03 * ratingConfidence)
            + (0.05 * recencyBonus);

    // Bonus multipliers
    if (profile.similarGameIds.contains(game.getIgdbId())) {
      score *= 1.30;
    }

    if (!profile.developerSlugs.isEmpty() && game.getDeveloperSlugs() != null) {
      for (String slug : game.getDeveloperSlugs()) {
        if (profile.developerSlugs.contains(slug)) {
          score *= 1.15;
          break;
        }
      }
    }

    // Multi-genre overlap bonus: 2+ genres with weight > 0.5
    long highWeightGenreOverlap =
        gameGenres.stream().filter(g -> profile.genreWeights.getOrDefault(g, 0.0) > 0.5).count();
    if (highWeightGenreOverlap >= 2) {
      score *= 1.10;
    }

    return score;
  }

  // ---- Diversity Pass ----

  private List<ScoredGame> diversify(List<ScoredGame> scored) {
    Map<String, Integer> genreThemeCounts = new HashMap<>();
    Map<String, Integer> developerCounts = new HashMap<>();
    Map<String, Integer> franchiseCounts = new HashMap<>();
    List<ScoredGame> diverse = new ArrayList<>();

    for (ScoredGame sg : scored) {
      String gtKey = genreThemeKey(sg.game);
      String devKey = developerKey(sg.game);
      String franKey = franchiseKey(sg.game);

      if (genreThemeCounts.getOrDefault(gtKey, 0) >= 3) continue;
      if (!devKey.isEmpty() && developerCounts.getOrDefault(devKey, 0) >= 3) continue;
      if (!franKey.isEmpty() && franchiseCounts.getOrDefault(franKey, 0) >= 2) continue;

      diverse.add(sg);
      genreThemeCounts.merge(gtKey, 1, Integer::sum);
      if (!devKey.isEmpty()) developerCounts.merge(devKey, 1, Integer::sum);
      if (!franKey.isEmpty()) franchiseCounts.merge(franKey, 1, Integer::sum);

      if (diverse.size() >= 20) break;
    }

    return diverse;
  }

  // ---- Filtering ----

  private boolean isInLikedFranchise(CachedGame game, PreferenceProfile profile) {
    if (profile.franchiseIds.isEmpty() || game.getFranchiseIds() == null) return false;
    for (Long fid : game.getFranchiseIds()) {
      if (profile.franchiseIds.contains(fid)) return true;
    }
    return false;
  }

  // ---- Similarity Math ----

  private double weightedJaccard(Map<Integer, Double> prefWeights, Set<Integer> gameSet) {
    if (prefWeights.isEmpty() && gameSet.isEmpty()) return 0.0;
    if (prefWeights.isEmpty() || gameSet.isEmpty()) return 0.0;

    double intersectionWeight = 0;
    double unionWeight = 0;
    Set<Integer> all = new HashSet<>(prefWeights.keySet());
    all.addAll(gameSet);

    for (int id : all) {
      double prefW = prefWeights.getOrDefault(id, 0.0);
      double gameW = gameSet.contains(id) ? 1.0 : 0.0;
      intersectionWeight += Math.min(prefW, gameW);
      unionWeight += Math.max(prefW, gameW);
    }
    return unionWeight > 0 ? intersectionWeight / unionWeight : 0;
  }

  private double jaccard(Set<Integer> a, Set<Integer> b) {
    if (a.isEmpty() && b.isEmpty()) return 0.0;
    if (a.isEmpty() || b.isEmpty()) return 0.0;
    Set<Integer> intersection = new HashSet<>(a);
    intersection.retainAll(b);
    Set<Integer> union = new HashSet<>(a);
    union.addAll(b);
    return (double) intersection.size() / union.size();
  }

  private boolean hasOverlap(Set<Integer> a, Set<Integer> b) {
    return a.stream().anyMatch(b::contains);
  }

  private double recencyBonus(LocalDate releaseDate) {
    if (releaseDate == null) return 0.0;
    long yearsAgo = ChronoUnit.YEARS.between(releaseDate, LocalDate.now());
    if (yearsAgo <= 3) return 1.0;
    if (yearsAgo <= 6) return 0.5;
    return 0.0;
  }

  // ---- Utility ----

  private Set<Integer> arrayToSet(Integer[] arr) {
    if (arr == null) return Set.of();
    return new HashSet<>(Arrays.asList(arr));
  }

  private <T> Set<T> toSet(List<T> list) {
    if (list == null) return Set.of();
    return new HashSet<>(list);
  }

  private String genreThemeKey(CachedGame game) {
    Integer[] genres = game.getGenreIds() != null ? game.getGenreIds().clone() : new Integer[0];
    Integer[] themes = game.getThemeIds() != null ? game.getThemeIds().clone() : new Integer[0];
    Arrays.sort(genres);
    Arrays.sort(themes);
    return Arrays.toString(genres) + "|" + Arrays.toString(themes);
  }

  private String developerKey(CachedGame game) {
    if (game.getDeveloperSlugs() == null || game.getDeveloperSlugs().length == 0) return "";
    String[] sorted = game.getDeveloperSlugs().clone();
    Arrays.sort(sorted);
    return Arrays.toString(sorted);
  }

  private String franchiseKey(CachedGame game) {
    if (game.getFranchiseIds() == null || game.getFranchiseIds().length == 0) return "";
    Long[] sorted = game.getFranchiseIds().clone();
    Arrays.sort(sorted);
    return Arrays.toString(sorted);
  }

  // ---- Inner Types ----

  record ScoredGame(CachedGame game, double score) {}

  record PreferenceProfile(
      Map<Integer, Double> genreWeights,
      Map<Integer, Double> themeWeights,
      Map<Integer, Double> keywordWeights,
      Set<Integer> gameModeIds,
      Set<Integer> perspectiveIds,
      Set<Integer> platformIds,
      Set<String> developerSlugs,
      Set<Long> franchiseIds,
      Set<Long> similarGameIds,
      Set<Long> likedGameIds) {}
}
