package com.gamerec.service;

import com.gamerec.model.domain.CachedGame;
import com.gamerec.model.dto.GameDto;
import com.gamerec.model.dto.PreferenceRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final GameCacheService cacheService;

    public RecommendationService(GameCacheService cacheService) {
        this.cacheService = cacheService;
    }

    public List<GameDto> getRecommendations(PreferenceRequest request) {
        List<CachedGame> candidates = cacheService.getCandidateGames();

        // If not enough candidates, fetch more from IGDB by preferred genres
        if (candidates.size() < 50 && request.genreIds() != null && !request.genreIds().isEmpty()) {
            candidates = cacheService.fetchAndCacheByGenres(request.genreIds(), 50);
        }

        Set<Integer> userGenres = toSet(request.genreIds());
        Set<Integer> userThemes = toSet(request.themeIds());
        Set<Integer> userPlatforms = toSet(request.platformIds());
        Set<Long> likedGameIds = request.likedGameIds() != null ? new HashSet<>(request.likedGameIds()) : Set.of();

        // Collect genre IDs from liked games for bonus scoring
        Set<Integer> likedGameGenres = candidates.stream()
                .filter(g -> likedGameIds.contains(g.getIgdbId()))
                .flatMap(g -> Arrays.stream(g.getGenreIds() != null ? g.getGenreIds() : new Integer[0]))
                .collect(Collectors.toSet());

        List<ScoredGame> scored = candidates.stream()
                .filter(g -> !likedGameIds.contains(g.getIgdbId()))
                .map(game -> new ScoredGame(game, scoreGame(game, userGenres, userThemes, userPlatforms, likedGameGenres)))
                .filter(sg -> sg.score > 0)
                .sorted(Comparator.comparingDouble(ScoredGame::score).reversed())
                .toList();

        // Diversity pass: max 3 games sharing the exact same genre set
        Map<String, Integer> genreSetCounts = new HashMap<>();
        List<ScoredGame> diverse = new ArrayList<>();
        for (ScoredGame sg : scored) {
            String key = genreKey(sg.game);
            int count = genreSetCounts.getOrDefault(key, 0);
            if (count < 3) {
                diverse.add(sg);
                genreSetCounts.put(key, count + 1);
            }
            if (diverse.size() >= 20) break;
        }

        return diverse.stream()
                .map(sg -> cacheService.toDto(sg.game, sg.score))
                .toList();
    }

    private double scoreGame(CachedGame game, Set<Integer> userGenres, Set<Integer> userThemes,
                             Set<Integer> userPlatforms, Set<Integer> likedGameGenres) {
        Set<Integer> gameGenres = arrayToSet(game.getGenreIds());
        Set<Integer> gameThemes = arrayToSet(game.getThemeIds());
        Set<Integer> gamePlatforms = arrayToSet(game.getPlatformIds());

        double genreOverlap = jaccard(userGenres, gameGenres);
        double themeOverlap = jaccard(userThemes, gameThemes);
        double platformMatch = hasOverlap(userPlatforms, gamePlatforms) ? 1.0 : 0.3;
        double normalizedRating = game.getRating() != null ? game.getRating() / 100.0 : 0.0;
        double recencyBonus = recencyBonus(game.getFirstRelease());

        double score = (0.35 * genreOverlap)
                + (0.25 * themeOverlap)
                + (0.15 * platformMatch)
                + (0.20 * normalizedRating)
                + (0.05 * recencyBonus);

        // Bonus for games sharing 2+ genres with liked games
        if (!likedGameGenres.isEmpty()) {
            long shared = gameGenres.stream().filter(likedGameGenres::contains).count();
            if (shared >= 2) {
                score *= 1.15;
            }
        }

        return Math.round(score * 1000.0) / 1000.0;
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
        if (a.isEmpty()) return true; // no platform preference = match all
        return a.stream().anyMatch(b::contains);
    }

    private double recencyBonus(LocalDate releaseDate) {
        if (releaseDate == null) return 0.0;
        long yearsAgo = java.time.temporal.ChronoUnit.YEARS.between(releaseDate, LocalDate.now());
        if (yearsAgo <= 3) return 1.0;
        if (yearsAgo <= 6) return 0.5;
        return 0.0;
    }

    private Set<Integer> arrayToSet(Integer[] arr) {
        if (arr == null) return Set.of();
        return new HashSet<>(Arrays.asList(arr));
    }

    private <T> Set<T> toSet(List<T> list) {
        if (list == null) return Set.of();
        return new HashSet<>(list);
    }

    private String genreKey(CachedGame game) {
        if (game.getGenreIds() == null) return "";
        Integer[] sorted = game.getGenreIds().clone();
        Arrays.sort(sorted);
        return Arrays.toString(sorted);
    }

    record ScoredGame(CachedGame game, double score) {}
}
