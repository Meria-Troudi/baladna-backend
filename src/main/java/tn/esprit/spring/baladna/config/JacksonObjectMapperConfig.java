package tn.esprit.spring.baladna.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4 may not register {@link ObjectMapper} as a bean by default with the current
 * Jackson setup; services that inject it (e.g. trip AI parsing) need an explicit bean.
 */
@Configuration
public class JacksonObjectMapperConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
