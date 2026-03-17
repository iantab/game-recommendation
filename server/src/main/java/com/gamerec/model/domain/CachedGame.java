package com.gamerec.model.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cached_games")
public class CachedGame {

    @Id
    @Column(name = "igdb_id")
    private Long igdbId;

    private String name;
    private String summary;
    private Double rating;

    @Column(name = "rating_count")
    private Integer ratingCount;

    @Column(name = "first_release")
    private LocalDate firstRelease;

    @Column(name = "cover_url")
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

    @Column(name = "raw_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String rawJson;

    @Column(name = "cached_at")
    private LocalDateTime cachedAt;

    public Long getIgdbId() { return igdbId; }
    public void setIgdbId(Long igdbId) { this.igdbId = igdbId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public Integer getRatingCount() { return ratingCount; }
    public void setRatingCount(Integer ratingCount) { this.ratingCount = ratingCount; }
    public LocalDate getFirstRelease() { return firstRelease; }
    public void setFirstRelease(LocalDate firstRelease) { this.firstRelease = firstRelease; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public Integer[] getGenreIds() { return genreIds; }
    public void setGenreIds(Integer[] genreIds) { this.genreIds = genreIds; }
    public Integer[] getThemeIds() { return themeIds; }
    public void setThemeIds(Integer[] themeIds) { this.themeIds = themeIds; }
    public Integer[] getPlatformIds() { return platformIds; }
    public void setPlatformIds(Integer[] platformIds) { this.platformIds = platformIds; }
    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
    public LocalDateTime getCachedAt() { return cachedAt; }
    public void setCachedAt(LocalDateTime cachedAt) { this.cachedAt = cachedAt; }
}
