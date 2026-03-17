package com.gamerec.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.gamerec.model.dto.GenreDto;
import com.gamerec.model.dto.PlatformDto;
import com.gamerec.model.dto.ThemeDto;
import com.gamerec.service.GameCacheService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ReferenceDataControllerTest {

  @Mock private GameCacheService cacheService;

  @InjectMocks private ReferenceDataController referenceDataController;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(referenceDataController).build();
  }

  @Test
  void getGenres_returns200WithList() throws Exception {
    when(cacheService.getAllGenres())
        .thenReturn(List.of(new GenreDto(12, "RPG", "rpg"), new GenreDto(1, "Action", "action")));

    mockMvc
        .perform(get("/api/genres"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(12))
        .andExpect(jsonPath("$[0].name").value("RPG"))
        .andExpect(jsonPath("$[1].name").value("Action"));
  }

  @Test
  void getThemes_returns200WithList() throws Exception {
    when(cacheService.getAllThemes()).thenReturn(List.of(new ThemeDto(17, "Fantasy", "fantasy")));

    mockMvc
        .perform(get("/api/themes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Fantasy"));
  }

  @Test
  void getPlatforms_returns200WithList() throws Exception {
    when(cacheService.getAllPlatforms()).thenReturn(List.of(new PlatformDto(6, "PC", "pc", 1)));

    mockMvc
        .perform(get("/api/platforms"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("PC"))
        .andExpect(jsonPath("$[0].category").value(1));
  }
}
