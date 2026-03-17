package com.gamerec.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.gamerec.model.dto.GameDto;
import com.gamerec.service.GameCacheService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class GameControllerTest {

  @Mock private GameCacheService cacheService;

  @InjectMocks private GameController gameController;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(gameController).build();
  }

  private static GameDto sampleGame() {
    return new GameDto(
        1L,
        "Elden Ring",
        "An action RPG",
        96.0,
        500,
        LocalDate.of(2022, 2, 25),
        "https://images.igdb.com/cover/1.jpg",
        List.of(),
        List.of(),
        List.of(),
        null);
  }

  @Test
  void searchGames_validRequest_returns200WithResults() throws Exception {
    when(cacheService.searchGames("elden", 10)).thenReturn(List.of(sampleGame()));

    mockMvc
        .perform(
            post("/api/games/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\": \"elden\", \"limit\": 10}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Elden Ring"))
        .andExpect(jsonPath("$[0].igdbId").value(1));
  }

  @Test
  void searchGames_nullLimit_defaultsToTwenty() throws Exception {
    when(cacheService.searchGames("test", 20)).thenReturn(List.of());

    mockMvc
        .perform(
            post("/api/games/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\": \"test\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  void getGame_found_returns200() throws Exception {
    when(cacheService.getGame(1L)).thenReturn(sampleGame());

    mockMvc
        .perform(get("/api/games/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Elden Ring"));
  }

  @Test
  void getGame_notFound_returns404() throws Exception {
    when(cacheService.getGame(999L)).thenReturn(null);

    mockMvc.perform(get("/api/games/999")).andExpect(status().isNotFound());
  }
}
