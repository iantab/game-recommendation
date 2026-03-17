package com.gamerec.repository;

import com.gamerec.model.domain.Theme;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThemeRepository extends JpaRepository<Theme, Integer> {}
