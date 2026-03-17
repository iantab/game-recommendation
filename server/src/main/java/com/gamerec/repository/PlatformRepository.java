package com.gamerec.repository;

import com.gamerec.model.domain.Platform;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformRepository extends JpaRepository<Platform, Integer> {
}
