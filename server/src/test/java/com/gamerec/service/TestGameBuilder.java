package com.gamerec.service;

import com.gamerec.model.domain.CachedGame;
import java.time.LocalDate;
import java.time.LocalDateTime;

final class TestGameBuilder {

  private final CachedGame game = new CachedGame();

  TestGameBuilder(long id) {
    game.setIgdbId(id);
    game.setName("Game " + id);
    game.setSummary("Summary for game " + id);
    game.setRating(75.0);
    game.setRatingCount(100);
    game.setFirstRelease(LocalDate.now().minusYears(2));
    game.setCoverUrl("https://images.igdb.com/cover/" + id + ".jpg");
    game.setGenreIds(new Integer[] {});
    game.setThemeIds(new Integer[] {});
    game.setPlatformIds(new Integer[] {});
    game.setSimilarGameIds(new Long[] {});
    game.setKeywordIds(new Integer[] {});
    game.setGameModeIds(new Integer[] {});
    game.setPerspectiveIds(new Integer[] {});
    game.setDeveloperSlugs(new String[] {});
    game.setFranchiseIds(new Long[] {});
    game.setCachedAt(LocalDateTime.now());
  }

  static TestGameBuilder game(long id) {
    return new TestGameBuilder(id);
  }

  TestGameBuilder name(String name) {
    game.setName(name);
    return this;
  }

  TestGameBuilder rating(double rating) {
    game.setRating(rating);
    return this;
  }

  TestGameBuilder ratingCount(int count) {
    game.setRatingCount(count);
    return this;
  }

  TestGameBuilder firstRelease(LocalDate date) {
    game.setFirstRelease(date);
    return this;
  }

  TestGameBuilder genres(Integer... ids) {
    game.setGenreIds(ids);
    return this;
  }

  TestGameBuilder themes(Integer... ids) {
    game.setThemeIds(ids);
    return this;
  }

  TestGameBuilder platforms(Integer... ids) {
    game.setPlatformIds(ids);
    return this;
  }

  TestGameBuilder keywords(Integer... ids) {
    game.setKeywordIds(ids);
    return this;
  }

  TestGameBuilder gameModes(Integer... ids) {
    game.setGameModeIds(ids);
    return this;
  }

  TestGameBuilder perspectives(Integer... ids) {
    game.setPerspectiveIds(ids);
    return this;
  }

  TestGameBuilder similarGames(Long... ids) {
    game.setSimilarGameIds(ids);
    return this;
  }

  TestGameBuilder developers(String... slugs) {
    game.setDeveloperSlugs(slugs);
    return this;
  }

  TestGameBuilder franchises(Long... ids) {
    game.setFranchiseIds(ids);
    return this;
  }

  TestGameBuilder cachedAt(LocalDateTime time) {
    game.setCachedAt(time);
    return this;
  }

  CachedGame build() {
    return game;
  }
}
