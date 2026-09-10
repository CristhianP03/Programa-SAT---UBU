package com.bienestar.sistema_bienestar_universitario.config;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class AppConfig {

    public AppConfig(Jackson2ObjectMapperBuilder builder) {
        builder.modulesToInstall(new Hibernate6Module());
    }

}