package com.gamerec.model.dto;

import java.util.List;

public record SearchRequest(String query, List<Integer> genreIds, List<Integer> platformIds, Integer limit) {
}
