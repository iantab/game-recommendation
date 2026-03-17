package com.gamerec.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.gamerec.model.dto.GameDto;
import com.gamerec.model.dto.PreferenceRequest;
import com.gamerec.service.RecommendationService;
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
class RecommendationControllerTest {

  @Mock private RecommendationService recommendationService;

  @InjectMocks private RecommendationController recommendationController;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(recommendationController).build();
  }

  @Test
  void getRecommendations_validRequest_returns200() throws Exception {
    GameDto game =
        new GameDto(
            10L,
            "Recommended Game",
            "Great game",
            88.0,
            200,
            LocalDate.of(2023, 6, 1),
            null,
            List.of(),
            List.of(),
            List.of(),
            0.85);
    when(recommendationService.getRecommendations(any(PreferenceRequest.class)))
        .thenReturn(List.of(game));

    mockMvc
        .perform(
            post("/api/recommendations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"genreIds\": [12], \"themeIds\": [17], \"platformIds\": [6], \"likedGameIds\": [1]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Recommended Game"))
        .andExpect(jsonPath("$[0].score").value(0.85));
  }

  @Test
  void getRecommendations_emptyBody_returns200() throws Exception {
    when(recommendationService.getRecommendations(any(PreferenceRequest.class)))
        .thenReturn(List.of());

    mockMvc
        .perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }
}
