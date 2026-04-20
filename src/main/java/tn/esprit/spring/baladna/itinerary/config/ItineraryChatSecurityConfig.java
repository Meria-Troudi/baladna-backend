package tn.esprit.spring.baladna.itinerary.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for Itinerary Chat endpoints.
 * 
 * This dedicated configuration ensures that WebSocket chat endpoints remain publicly accessible
 * even if the main SecurityConfig in the user package gets reverted during backend merges.
 * 
 * Order: 1 (high priority - processed before main SecurityConfig which has default order of -1)
 * The securityMatcher() ensures this config only applies to /api/chat/** requests.
 * 
 * JWT validation still occurs at the WebSocket handshake interceptor level.
 */
@Configuration
@Order(1)
public class ItineraryChatSecurityConfig {

    @Bean(name = "itineraryChatSecurityFilterChain")
    public SecurityFilterChain chatSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/chat/**")
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
