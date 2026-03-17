package com.gamerec.model.igdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IgdbGame(
        Long id,
        String name,
        String summary,
        Double rating,
        @JsonProperty("rating_count") Integer ratingCount,
        @JsonProperty("first_release_date") Long firstReleaseDate,
        Cover cover,
        List<IgdbRef> genres,
        List<IgdbRef> themes,
        List<IgdbRef> platforms,
        @JsonProperty("similar_games") List<Long> similarGames,
        @JsonProperty("game_modes") List<IgdbRef> gameModes,
        @JsonProperty("player_perspectives") List<IgdbRef> playerPerspectives,
        List<IgdbRef> keywords,
        @JsonProperty("involved_companies") List<InvolvedCompany> involvedCompanies,
        List<IgdbRef> franchises
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cover(Long id, @JsonProperty("image_id") String imageId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InvolvedCompany(
            IgdbRef company,
            boolean developer,
            boolean publisher
    ) {
    }
}
