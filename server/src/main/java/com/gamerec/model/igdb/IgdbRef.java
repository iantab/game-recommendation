package com.gamerec.model.igdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IgdbRef(Integer id, String name, String slug, Integer category) {
}
