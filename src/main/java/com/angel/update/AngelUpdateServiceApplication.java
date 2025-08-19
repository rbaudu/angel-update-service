package com.angel.update;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application principale du service de mise à jour ANGEL
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableAsync
public class AngelUpdateServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AngelUpdateServiceApplication.class, args);
    }
}
