package com.gamerec.service;

import tools.jackson.databind.ObjectMapper;
import com.gamerec.model.domain.*;
import com.gamerec.model.dto.*;
import com.gamerec.model.igdb.IgdbGame;
import com.gamerec.model.igdb.IgdbRef;
import com.gamerec.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class GameCacheService {

    private static final Logger log = LoggerFactory.getLogger(GameCacheService.class);

    private final IgdbClientService igdbClient;
    private final CachedGameRepository gameRepo;
    private final GenreRepository genreRepo;
    private final ThemeRepository themeRepo;
    private final PlatformRepository platformRepo;
    private final ObjectMapper objectMapper;

    @Value("${game-cache.ttl-days:7}")
    private int ttlDays;

    public GameCacheService(IgdbClientService igdbClient, CachedGameRepository gameRepo,
                            GenreRepository genreRepo, ThemeRepository themeRepo,
                            PlatformRepository platformRepo, ObjectMapper objectMapper) {
        this.igdbClient = igdbClient;
        this.gameRepo = gameRepo;
        this.genreRepo = genreRepo;
        this.themeRepo = themeRepo;
        this.platformRepo = platformRepo;
        this.objectMapper = objectMapper;
    }

    public List<GameDto> searchGames(String query, int limit) {
        List<IgdbGame> games = igdbClient.searchGames(query, limit);
        games.forEach(this::cacheGame);
        return games.stream().map(this::toDto).toList();
    }

    public GameDto getGame(long igdbId) {
        Optional<CachedGame> cached = gameRepo.findById(igdbId);
        if (cached.isPresent() && !isExpired(cached.get())) {
            return toDto(cached.get(), null);
        }
        IgdbGame game = igdbClient.getGame(igdbId);
        if (game == null) return null;
        cacheGame(game);
        return toDto(game);
    }

    public List<CachedGame> getCandidateGames() {
        return gameRepo.findByCachedAtAfter(LocalDateTime.now().minusDays(ttlDays));
    }

    public List<CachedGame> fetchAndCacheByGenres(List<Integer> genreIds, int limit) {
        List<IgdbGame> games = igdbClient.getGamesByGenres(genreIds, limit);
        games.forEach(this::cacheGame);
        return gameRepo.findByCachedAtAfter(LocalDateTime.now().minusDays(ttlDays));
    }

    public void loadReferenceData() {
        if (genreRepo.count() == 0) {
            log.info("Loading genres from IGDB");
            List<IgdbRef> genres = igdbClient.getGenres();
            genres.forEach(g -> genreRepo.save(new Genre(g.id(), g.name(), g.slug())));
        }
        if (themeRepo.count() == 0) {
            log.info("Loading themes from IGDB");
            List<IgdbRef> themes = igdbClient.getThemes();
            themes.forEach(t -> themeRepo.save(new Theme(t.id(), t.name(), t.slug())));
        }
        if (platformRepo.count() == 0) {
            log.info("Loading platforms from IGDB");
            List<IgdbRef> platforms = igdbClient.getPlatforms();
            platforms.forEach(p -> platformRepo.save(new Platform(p.id(), p.name(), p.slug(), p.category())));
        }
    }

    public List<GenreDto> getAllGenres() {
        return genreRepo.findAll().stream()
                .map(g -> new GenreDto(g.getIgdbId(), g.getName(), g.getSlug()))
                .toList();
    }

    public List<ThemeDto> getAllThemes() {
        return themeRepo.findAll().stream()
                .map(t -> new ThemeDto(t.getIgdbId(), t.getName(), t.getSlug()))
                .toList();
    }

    public List<PlatformDto> getAllPlatforms() {
        return platformRepo.findAll().stream()
                .map(p -> new PlatformDto(p.getIgdbId(), p.getName(), p.getSlug(), p.getCategory()))
                .toList();
    }

    public GameDto toDto(CachedGame game, Double score) {
        return new GameDto(
                game.getIgdbId(), game.getName(), game.getSummary(),
                game.getRating(), game.getRatingCount(), game.getFirstRelease(), game.getCoverUrl(),
                resolveGenres(game.getGenreIds()),
                resolveThemes(game.getThemeIds()),
                resolvePlatforms(game.getPlatformIds()),
                score
        );
    }

    private void cacheGame(IgdbGame game) {
        try {
            CachedGame cached = new CachedGame();
            cached.setIgdbId(game.id());
            cached.setName(game.name());
            cached.setSummary(game.summary());
            cached.setRating(game.rating());
            cached.setRatingCount(game.ratingCount());
            if (game.firstReleaseDate() != null) {
                cached.setFirstRelease(
                        Instant.ofEpochSecond(game.firstReleaseDate()).atZone(ZoneOffset.UTC).toLocalDate());
            }
            if (game.cover() != null && game.cover().imageId() != null) {
                cached.setCoverUrl("https://images.igdb.com/igdb/image/upload/t_cover_big/"
                        + game.cover().imageId() + ".jpg");
            }
            cached.setGenreIds(toIdArray(game.genres()));
            cached.setThemeIds(toIdArray(game.themes()));
            cached.setPlatformIds(toIdArray(game.platforms()));
            cached.setRawJson(objectMapper.writeValueAsString(game));
            cached.setCachedAt(LocalDateTime.now());
            gameRepo.save(cached);
        } catch (Exception e) {
            log.warn("Failed to cache game {}: {}", game.id(), e.getMessage());
        }
    }

    private GameDto toDto(IgdbGame game) {
        return new GameDto(
                game.id(), game.name(), game.summary(), game.rating(), game.ratingCount(),
                game.firstReleaseDate() != null
                        ? Instant.ofEpochSecond(game.firstReleaseDate()).atZone(ZoneOffset.UTC).toLocalDate()
                        : null,
                game.cover() != null && game.cover().imageId() != null
                        ? "https://images.igdb.com/igdb/image/upload/t_cover_big/" + game.cover().imageId() + ".jpg"
                        : null,
                game.genres() != null ? game.genres().stream().map(g -> new GenreDto(g.id(), g.name(), g.slug())).toList() : List.of(),
                game.themes() != null ? game.themes().stream().map(t -> new ThemeDto(t.id(), t.name(), t.slug())).toList() : List.of(),
                game.platforms() != null ? game.platforms().stream().map(p -> new PlatformDto(p.id(), p.name(), p.slug(), p.category())).toList() : List.of(),
                null
        );
    }

    private boolean isExpired(CachedGame game) {
        return game.getCachedAt().isBefore(LocalDateTime.now().minusDays(ttlDays));
    }

    private Integer[] toIdArray(List<IgdbRef> refs) {
        if (refs == null) return new Integer[0];
        return refs.stream().map(IgdbRef::id).toArray(Integer[]::new);
    }

    private List<GenreDto> resolveGenres(Integer[] ids) {
        if (ids == null || ids.length == 0) return List.of();
        return genreRepo.findAllById(Arrays.asList(ids)).stream()
                .map(g -> new GenreDto(g.getIgdbId(), g.getName(), g.getSlug()))
                .toList();
    }

    private List<ThemeDto> resolveThemes(Integer[] ids) {
        if (ids == null || ids.length == 0) return List.of();
        return themeRepo.findAllById(Arrays.asList(ids)).stream()
                .map(t -> new ThemeDto(t.getIgdbId(), t.getName(), t.getSlug()))
                .toList();
    }

    private List<PlatformDto> resolvePlatforms(Integer[] ids) {
        if (ids == null || ids.length == 0) return List.of();
        return platformRepo.findAllById(Arrays.asList(ids)).stream()
                .map(p -> new PlatformDto(p.getIgdbId(), p.getName(), p.getSlug(), p.getCategory()))
                .toList();
    }
}
