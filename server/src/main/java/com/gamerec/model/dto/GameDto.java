package com.gamerec.model.dto;

import java.time.LocalDate;
import java.util.List;

public record GameDto(
        Long igdbId,
        String name,
        String summary,
        Double rating,
        Integer ratingCount,
        LocalDate firstRelease,
        String coverUrl,
        List<GenreDto> genres,
        List<ThemeDto> themes,
        List<PlatformDto> platforms,
        Double score
) {
}
