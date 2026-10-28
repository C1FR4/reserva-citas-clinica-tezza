package com.clinica.clinica.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class CorreoConfig {
    @Bean
    Clock relojClinica() {
        return Clock.system(ZoneId.of("America/Lima"));
    }
}
