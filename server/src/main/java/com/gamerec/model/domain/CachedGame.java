package com.gamerec.model.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "cached_games")
public class CachedGame {

  @Id
  @Column(name = "igdb_id")
  private Long igdbId;

  @Column(columnDefinition = "text")
  private String name;

  @Column(columnDefinition = "text")
  private String summary;

  private Double rating;

  @Column(name = "rating_count")
  private Integer ratingCount;

  @Column(name = "first_release")
  private LocalDate firstRelease;

  @Column(name = "cover_url", columnDefinition = "text")
  private String coverUrl;

  @Column(name = "genre_ids", columnDefinition = "integer[]")
  @JdbcTypeCode(SqlTypes.ARRAY)
  private Integer[] genreIds;

  @Column(name = "theme_ids", columnDefinition = "integer[]")
  @JdbcTypeCode(SqlTypes.ARRAY)
  private Integer[] themeIds;

  @Column(name = "platform_ids", columnDefinition = "integer[]")
  @JdbcTypeCode(SqlTypes.ARRAY)
  private Integer[] platformIds;

  @Column(name = "similar_game_ids", columnDefinition = "bigint[]")
  @JdbcTypeCode(SqlTypes.ARRAY)
  private Long[] similarGameIds;

  @Column(name = "keyword_ids", columnDefinition = "integer[]")
  @JdbcTypeCode(SqlTypes.ARRAY)
  private Integer[] keywordIds;

  @Column(name = "game_mode_ids", columnDefinition = "integer[]")
  @JdbcTypeCode(SqlTypes.ARRAY)
  private Integer[] gameModeIds;

  @Column(name = "perspective_ids", columnDefinition = "integer[]")
  @JdbcTypeCode(SqlTypes.ARRAY)
  private Integer[] perspectiveIds;

  @Column(name = "developer_slugs", columnDefinition = "text[]")
  @JdbcTypeCode(SqlTypes.ARRAY)
  private String[] developerSlugs;

  @Column(name = "franchise_ids", columnDefinition = "bigint[]")
  @JdbcTypeCode(SqlTypes.ARRAY)
  private Long[] franchiseIds;

  @Column(name = "raw_json", columnDefinition = "text")
  private String rawJson;

  @Column(name = "cached_at")
  private LocalDateTime cachedAt;

  public Long getIgdbId() {
    return igdbId;
  }

  public void setIgdbId(Long igdbId) {
    this.igdbId = igdbId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public Double getRating() {
    return rating;
  }

  public void setRating(Double rating) {
    this.rating = rating;
  }

  public Integer getRatingCount() {
    return ratingCount;
  }

  public void setRatingCount(Integer ratingCount) {
    this.ratingCount = ratingCount;
  }

  public LocalDate getFirstRelease() {
    return firstRelease;
  }

  public void setFirstRelease(LocalDate firstRelease) {
    this.firstRelease = firstRelease;
  }

  public String getCoverUrl() {
    return coverUrl;
  }

  public void setCoverUrl(String coverUrl) {
    this.coverUrl = coverUrl;
  }

  public Integer[] getGenreIds() {
    return genreIds;
  }

  public void setGenreIds(Integer[] genreIds) {
    this.genreIds = genreIds;
  }

  public Integer[] getThemeIds() {
    return themeIds;
  }

  public void setThemeIds(Integer[] themeIds) {
    this.themeIds = themeIds;
  }

  public Integer[] getPlatformIds() {
    return platformIds;
  }

  public void setPlatformIds(Integer[] platformIds) {
    this.platformIds = platformIds;
  }

  public Long[] getSimilarGameIds() {
    return similarGameIds;
  }

  public void setSimilarGameIds(Long[] similarGameIds) {
    this.similarGameIds = similarGameIds;
  }

  public Integer[] getKeywordIds() {
    return keywordIds;
  }

  public void setKeywordIds(Integer[] keywordIds) {
    this.keywordIds = keywordIds;
  }

  public Integer[] getGameModeIds() {
    return gameModeIds;
  }

  public void setGameModeIds(Integer[] gameModeIds) {
    this.gameModeIds = gameModeIds;
  }

  public Integer[] getPerspectiveIds() {
    return perspectiveIds;
  }

  public void setPerspectiveIds(Integer[] perspectiveIds) {
    this.perspectiveIds = perspectiveIds;
  }

  public String[] getDeveloperSlugs() {
    return developerSlugs;
  }

  public void setDeveloperSlugs(String[] developerSlugs) {
    this.developerSlugs = developerSlugs;
  }

  public Long[] getFranchiseIds() {
    return franchiseIds;
  }

  public void setFranchiseIds(Long[] franchiseIds) {
    this.franchiseIds = franchiseIds;
  }

  public String getRawJson() {
    return rawJson;
  }

  public void setRawJson(String rawJson) {
    this.rawJson = rawJson;
  }

  public LocalDateTime getCachedAt() {
    return cachedAt;
  }

  public void setCachedAt(LocalDateTime cachedAt) {
    this.cachedAt = cachedAt;
  }
}
