package com.gamerec.controller;

import com.gamerec.model.dto.GameDto;
import com.gamerec.model.dto.SearchRequest;
import com.gamerec.service.GameCacheService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games")
public class GameController {

  private final GameCacheService cacheService;

  public GameController(GameCacheService cacheService) {
    this.cacheService = cacheService;
  }

  @PostMapping("/search")
  public List<GameDto> searchGames(@RequestBody SearchRequest request) {
    return cacheService.searchGames(
        request.query(), request.limit() != null ? request.limit() : 20);
  }

  @GetMapping("/{id}")
  public ResponseEntity<GameDto> getGame(@PathVariable Long id) {
    GameDto game = cacheService.getGame(id);
    return game != null ? ResponseEntity.ok(game) : ResponseEntity.notFound().build();
  }
}
