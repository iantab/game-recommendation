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
        List<IgdbRef> platforms
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cover(Long id, @JsonProperty("image_id") String imageId) {
    }
}
