package com.gamerec.controller;

import com.gamerec.model.dto.GenreDto;
import com.gamerec.model.dto.PlatformDto;
import com.gamerec.model.dto.ThemeDto;
import com.gamerec.service.GameCacheService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReferenceDataController {

    private final GameCacheService cacheService;

    public ReferenceDataController(GameCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @GetMapping("/genres")
    public List<GenreDto> getGenres() {
        return cacheService.getAllGenres();
    }

    @GetMapping("/themes")
    public List<ThemeDto> getThemes() {
        return cacheService.getAllThemes();
    }

    @GetMapping("/platforms")
    public List<PlatformDto> getPlatforms() {
        return cacheService.getAllPlatforms();
    }
}
