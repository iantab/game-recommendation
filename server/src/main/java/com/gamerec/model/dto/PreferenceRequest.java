package com.gamerec.model.dto;

import java.util.List;

public record PreferenceRequest(
        List<Integer> genreIds,
        List<Integer> themeIds,
        List<Integer> platformIds,
        List<Long> likedGameIds
) {
}
