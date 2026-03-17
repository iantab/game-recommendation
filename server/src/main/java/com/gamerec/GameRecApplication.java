package com.gamerec;

import com.gamerec.service.GameCacheService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GameRecApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameRecApplication.class, args);
    }

    @Bean
    ApplicationRunner loadReferenceData(GameCacheService cacheService) {
        return args -> cacheService.loadReferenceData();
    }
}
