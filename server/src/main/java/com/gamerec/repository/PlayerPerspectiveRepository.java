package com.gamerec.repository;

import com.gamerec.model.domain.PlayerPerspective;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerPerspectiveRepository extends JpaRepository<PlayerPerspective, Integer> {
}
