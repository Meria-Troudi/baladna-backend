package tn.esprit.spring.baladna.user.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth

                        // SWAGGER
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // AUTH
                        .requestMatchers("/api/auth/**").permitAll()

                        // PROFILE
                        .requestMatchers("/api/profile/**").authenticated()

                        // STATIONS
                        .requestMatchers(HttpMethod.GET, "/api/stations/**").hasAnyRole("HOST", "TOURIST")
                        .requestMatchers(HttpMethod.POST, "/api/stations/**").hasRole("HOST")
                        .requestMatchers(HttpMethod.PUT, "/api/stations/**").hasRole("HOST")
                        .requestMatchers(HttpMethod.DELETE, "/api/stations/**").hasRole("HOST")

                        // TRAJETS
                        .requestMatchers(HttpMethod.GET, "/api/trajets/**").hasAnyRole("HOST", "TOURIST")
                        .requestMatchers(HttpMethod.POST, "/api/trajets/**").hasRole("HOST")
                        .requestMatchers(HttpMethod.PUT, "/api/trajets/**").hasRole("HOST")
                        .requestMatchers(HttpMethod.DELETE, "/api/trajets/**").hasRole("HOST")

                        // TRANSPORTS
                        .requestMatchers(HttpMethod.GET, "/api/transports/**").hasAnyRole("HOST", "TOURIST")
                        .requestMatchers(HttpMethod.POST, "/api/transports/**").hasRole("HOST")
                        .requestMatchers(HttpMethod.PUT, "/api/transports/**").hasRole("HOST")
                        .requestMatchers(HttpMethod.DELETE, "/api/transports/**").hasRole("HOST")

                        // RESERVATIONS
                        // TOURIST
                        .requestMatchers(HttpMethod.GET, "/api/reservations/me").hasRole("TOURIST")
                        .requestMatchers(HttpMethod.POST, "/api/reservations").hasRole("TOURIST")
                        .requestMatchers(HttpMethod.PUT, "/api/reservations/*/cancel").hasRole("TOURIST")

                        // HOST
                        .requestMatchers(HttpMethod.GET, "/api/reservations/**").hasRole("HOST")
                        .requestMatchers(HttpMethod.DELETE, "/api/reservations/**").hasRole("HOST")

                        // tout le reste
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("http://localhost:4200", "http://localhost:8081"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}