package com.gamerec.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.gamerec.config.IgdbConfig;
import com.gamerec.model.igdb.IgdbGame;
import com.gamerec.model.igdb.IgdbRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class IgdbClientService {

    private static final Logger log = LoggerFactory.getLogger(IgdbClientService.class);

    private static final String GAME_FIELDS =
            "fields name, summary, rating, rating_count, first_release_date, "
            + "cover.image_id, genres.name, genres.slug, themes.name, themes.slug, "
            + "platforms.name, platforms.slug, platforms.category;";

    private final IgdbConfig config;
    private final IgdbTokenManager tokenManager;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private long lastRequestMs = 0;

    public IgdbClientService(IgdbConfig config, IgdbTokenManager tokenManager, ObjectMapper objectMapper) {
        this.config = config;
        this.tokenManager = tokenManager;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    public List<IgdbGame> searchGames(String query, int limit) {
        String body = GAME_FIELDS + " search \"" + sanitize(query) + "\"; limit " + clampLimit(limit) + ";";
        return queryGames(body);
    }

    public IgdbGame getGame(long igdbId) {
        String body = GAME_FIELDS + " where id = " + igdbId + ";";
        List<IgdbGame> results = queryGames(body);
        return results.isEmpty() ? null : results.getFirst();
    }

    public List<IgdbGame> getGamesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        String idList = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        String body = GAME_FIELDS + " where id = (" + idList + ");";
        return queryGames(body);
    }

    public List<IgdbGame> getGamesByGenres(List<Integer> genreIds, int limit) {
        if (genreIds == null || genreIds.isEmpty()) return List.of();
        String idList = genreIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String body = GAME_FIELDS
                + " where genres = (" + idList + ") & rating > 60 & rating_count > 5;"
                + " sort rating desc; limit " + clampLimit(limit) + ";";
        return queryGames(body);
    }

    public List<IgdbRef> getGenres() {
        return queryRefs("/genres", "fields name, slug; limit 500;");
    }

    public List<IgdbRef> getThemes() {
        return queryRefs("/themes", "fields name, slug; limit 500;");
    }

    public List<IgdbRef> getPlatforms() {
        return queryRefs("/platforms", "fields name, slug, category; limit 500;");
    }

    private List<IgdbGame> queryGames(String apicalypse) {
        return query("/games", apicalypse, new TypeReference<>() {});
    }

    private List<IgdbRef> queryRefs(String endpoint, String apicalypse) {
        return query(endpoint, apicalypse, new TypeReference<>() {});
    }

    private <T> T query(String endpoint, String apicalypse, TypeReference<T> typeRef) {
        rateLimit();
        log.debug("IGDB {} -> {}", endpoint, apicalypse);
        String json = restClient.post()
                .uri(config.baseUrl() + endpoint)
                .header("Client-ID", config.clientId())
                .header("Authorization", "Bearer " + tokenManager.getAccessToken())
                .contentType(MediaType.TEXT_PLAIN)
                .body(apicalypse)
                .retrieve()
                .body(String.class);
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse IGDB response for " + endpoint, e);
        }
    }

    private synchronized void rateLimit() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRequestMs;
        if (elapsed < 250) {
            try {
                Thread.sleep(250 - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestMs = System.currentTimeMillis();
    }

    private String sanitize(String input) {
        return input.replace("\"", "\\\"").replace(";", "");
    }

    private int clampLimit(int limit) {
        return Math.max(1, Math.min(limit <= 0 ? 20 : limit, 50));
    }
}
