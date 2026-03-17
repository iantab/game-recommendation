package com.gamerec.repository;

import com.gamerec.model.domain.CachedGame;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CachedGameRepository extends JpaRepository<CachedGame, Long> {

    List<CachedGame> findByCachedAtAfter(LocalDateTime cutoff);
}
