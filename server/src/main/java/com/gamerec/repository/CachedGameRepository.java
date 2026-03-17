package com.gamerec.repository;

import com.gamerec.model.domain.CachedGame;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CachedGameRepository extends JpaRepository<CachedGame, Long> {

  List<CachedGame> findByCachedAtAfter(LocalDateTime cutoff);
}
