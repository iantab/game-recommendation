package com.gamerec.model.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "themes")
public class Theme {

    @Id
    @Column(name = "igdb_id")
    private Integer igdbId;

    private String name;
    private String slug;

    public Theme() {}

    public Theme(Integer igdbId, String name, String slug) {
        this.igdbId = igdbId;
        this.name = name;
        this.slug = slug;
    }

    public Integer getIgdbId() { return igdbId; }
    public void setIgdbId(Integer igdbId) { this.igdbId = igdbId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
}
