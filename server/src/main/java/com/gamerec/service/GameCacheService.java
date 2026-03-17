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
    private final GameModeRepository gameModeRepo;
    private final PlayerPerspectiveRepository perspectiveRepo;
    private final ObjectMapper objectMapper;

    @Value("${game-cache.ttl-days:7}")
    private int ttlDays;

    public GameCacheService(IgdbClientService igdbClient, CachedGameRepository gameRepo,
                            GenreRepository genreRepo, ThemeRepository themeRepo,
                            PlatformRepository platformRepo, GameModeRepository gameModeRepo,
                            PlayerPerspectiveRepository perspectiveRepo, ObjectMapper objectMapper) {
        this.igdbClient = igdbClient;
        this.gameRepo = gameRepo;
        this.genreRepo = genreRepo;
        this.themeRepo = themeRepo;
        this.platformRepo = platformRepo;
        this.gameModeRepo = gameModeRepo;
        this.perspectiveRepo = perspectiveRepo;
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

    public List<CachedGame> fetchAndCacheByGenresAndThemes(List<Integer> genreIds, List<Integer> themeIds, int limit) {
        List<IgdbGame> games = igdbClient.getGamesByGenresAndThemes(genreIds, themeIds, limit);
        games.forEach(this::cacheGame);
        return games.stream()
                .map(g -> gameRepo.findById(g.id()).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Ensures liked games are cached and fetches their similar games.
     * Returns the list of similar games as CachedGame objects.
     */
    public List<CachedGame> fetchSimilarGamesForLiked(List<Long> likedGameIds) {
        if (likedGameIds == null || likedGameIds.isEmpty()) return List.of();

        // Ensure liked games are in cache
        List<Long> uncachedIds = likedGameIds.stream()
                .filter(id -> gameRepo.findById(id).map(this::isExpired).orElse(true))
                .toList();
        if (!uncachedIds.isEmpty()) {
            igdbClient.getGamesByIds(uncachedIds).forEach(this::cacheGame);
        }

        // Collect similar game IDs from liked games
        Set<Long> similarIds = new LinkedHashSet<>();
        for (Long likedId : likedGameIds) {
            gameRepo.findById(likedId).ifPresent(game -> {
                if (game.getSimilarGameIds() != null) {
                    for (Long simId : game.getSimilarGameIds()) {
                        if (!likedGameIds.contains(simId)) {
                            similarIds.add(simId);
                        }
                    }
                }
            });
        }

        if (similarIds.isEmpty()) return List.of();

        // Fetch uncached similar games
        List<Long> uncachedSimilar = similarIds.stream()
                .filter(id -> gameRepo.findById(id).map(this::isExpired).orElse(true))
                .toList();
        if (!uncachedSimilar.isEmpty()) {
            // Batch in groups of 50 (IGDB limit)
            for (int i = 0; i < uncachedSimilar.size(); i += 50) {
                List<Long> batch = uncachedSimilar.subList(i, Math.min(i + 50, uncachedSimilar.size()));
                igdbClient.getGamesByIds(batch).forEach(this::cacheGame);
            }
        }

        return similarIds.stream()
                .map(gameRepo::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    /**
     * Returns cached versions of the given game IDs, fetching from IGDB if needed.
     */
    public List<CachedGame> ensureCached(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        List<Long> uncached = ids.stream()
                .filter(id -> gameRepo.findById(id).map(this::isExpired).orElse(true))
                .toList();
        if (!uncached.isEmpty()) {
            for (int i = 0; i < uncached.size(); i += 50) {
                List<Long> batch = uncached.subList(i, Math.min(i + 50, uncached.size()));
                igdbClient.getGamesByIds(batch).forEach(this::cacheGame);
            }
        }

        return ids.stream()
                .map(gameRepo::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
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
        if (gameModeRepo.count() == 0) {
            log.info("Loading game modes from IGDB");
            List<IgdbRef> modes = igdbClient.getGameModes();
            modes.forEach(m -> gameModeRepo.save(new GameMode(m.id(), m.name(), m.slug())));
        }
        if (perspectiveRepo.count() == 0) {
            log.info("Loading player perspectives from IGDB");
            List<IgdbRef> perspectives = igdbClient.getPlayerPerspectives();
            perspectives.forEach(p -> perspectiveRepo.save(new PlayerPerspective(p.id(), p.name(), p.slug())));
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
            cached.setSimilarGameIds(game.similarGames() != null
                    ? game.similarGames().toArray(Long[]::new) : new Long[0]);
            cached.setKeywordIds(toIdArray(game.keywords()));
            cached.setGameModeIds(toIdArray(game.gameModes()));
            cached.setPerspectiveIds(toIdArray(game.playerPerspectives()));
            cached.setDeveloperSlugs(extractDeveloperSlugs(game));
            cached.setFranchiseIds(extractFranchiseIds(game));
            cached.setRawJson(objectMapper.writeValueAsString(game));
            cached.setCachedAt(LocalDateTime.now());
            gameRepo.save(cached);
        } catch (Exception e) {
            log.error("Failed to cache game {}: {}", game.id(), e.getMessage(), e);
        }
    }

    private String[] extractDeveloperSlugs(IgdbGame game) {
        if (game.involvedCompanies() == null) return new String[0];
        return game.involvedCompanies().stream()
                .filter(ic -> ic.developer() && ic.company() != null)
                .map(ic -> ic.company().slug())
                .filter(Objects::nonNull)
                .toArray(String[]::new);
    }

    private Long[] extractFranchiseIds(IgdbGame game) {
        if (game.franchises() == null) return new Long[0];
        return game.franchises().stream()
                .map(f -> (long) f.id())
                .toArray(Long[]::new);
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
