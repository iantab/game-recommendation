package com.gamerec.model.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "platforms")
public class Platform {

    @Id
    @Column(name = "igdb_id")
    private Integer igdbId;

    private String name;
    private String slug;
    private Integer category;

    public Platform() {}

    public Platform(Integer igdbId, String name, String slug, Integer category) {
        this.igdbId = igdbId;
        this.name = name;
        this.slug = slug;
        this.category = category;
    }

    public Integer getIgdbId() { return igdbId; }
    public void setIgdbId(Integer igdbId) { this.igdbId = igdbId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public Integer getCategory() { return category; }
    public void setCategory(Integer category) { this.category = category; }
}
