package com.gamerec.repository;

import com.gamerec.model.domain.GameMode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameModeRepository extends JpaRepository<GameMode, Integer> {
}
