package com.gamerec.controller;

import com.gamerec.model.dto.GameDto;
import com.gamerec.model.dto.PreferenceRequest;
import com.gamerec.service.RecommendationService;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RecommendationController {

  private final RecommendationService recommendationService;

  public RecommendationController(RecommendationService recommendationService) {
    this.recommendationService = recommendationService;
  }

  @PostMapping("/recommendations")
  public List<GameDto> getRecommendations(@RequestBody PreferenceRequest request) {
    return recommendationService.getRecommendations(request);
  }
}
