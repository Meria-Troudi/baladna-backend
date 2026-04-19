package tn.esprit.spring.baladna.user.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
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
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()
                        .requestMatchers("/api/events/**").permitAll()

                        .requestMatchers("/api/profile/**").authenticated()

                        .requestMatchers("/api/users/**").hasAnyAuthority("ADMIN", "ROLE_ADMIN")
                        .requestMatchers("/api/dashboard-admin/**").hasAnyAuthority("ADMIN", "ROLE_ADMIN")

                        .requestMatchers("/api/artisan/**").hasAnyAuthority("ARTISAN", "ROLE_ARTISAN")

                        .requestMatchers(HttpMethod.GET, "/api/stations/**")
                        .hasAnyAuthority("HOST", "ROLE_HOST", "TOURIST", "ROLE_TOURIST")
                        .requestMatchers(HttpMethod.POST, "/api/stations/**")
                        .hasAnyAuthority("HOST", "ROLE_HOST")
                        .requestMatchers(HttpMethod.PUT, "/api/stations/**")
                        .hasAnyAuthority("HOST", "ROLE_HOST")
                        .requestMatchers(HttpMethod.DELETE, "/api/stations/**")
                        .hasAnyAuthority("HOST", "ROLE_HOST")

                        .requestMatchers(HttpMethod.GET, "/api/trajets/**")
                        .hasAnyAuthority("HOST", "ROLE_HOST", "TOURIST", "ROLE_TOURIST")
                        .requestMatchers(HttpMethod.POST, "/api/trajets/**")
                        .hasAnyAuthority("HOST", "ROLE_HOST")
                        .requestMatchers(HttpMethod.PUT, "/api/trajets/**")
                        .hasAnyAuthority("HOST", "ROLE_HOST")
                        .requestMatchers(HttpMethod.DELETE, "/api/trajets/**")
                        .hasAnyAuthority("HOST", "ROLE_HOST")

                        .requestMatchers(HttpMethod.GET, "/api/transports/**")
                        .hasAnyAuthority("HOST", "ROLE_HOST", "TOURIST", "ROLE_TOURIST")
                        .requestMatchers(HttpMethod.POST, "/api/transports/**")
                        .hasAnyAuthority("HOST", "ROLE_HOST")
                        .requestMatchers(HttpMethod.PUT, "/api/transports/**")
                        .hasAnyAuthority("HOST", "ROLE_HOST")
                        .requestMatchers(HttpMethod.DELETE, "/api/transports/**")
                        .hasAnyAuthority("HOST", "ROLE_HOST")

                        .requestMatchers(HttpMethod.GET, "/api/reservations")
                        .hasAnyAuthority("HOST", "ROLE_HOST", "TOURIST", "ROLE_TOURIST")
                        .requestMatchers(HttpMethod.GET, "/api/reservations/me")
                        .hasAnyAuthority("TOURIST", "ROLE_TOURIST")
                        .requestMatchers(HttpMethod.POST, "/api/reservations")
                        .hasAnyAuthority("TOURIST", "ROLE_TOURIST")
                        .requestMatchers(HttpMethod.PUT, "/api/reservations/*/cancel")
                        .hasAnyAuthority("TOURIST", "ROLE_TOURIST")

                        .requestMatchers(HttpMethod.PUT, "/api/reservations/*/approve")
                        .hasAnyAuthority("HOST", "ROLE_HOST")
                        .requestMatchers(HttpMethod.PUT, "/api/reservations/*/reject")
                        .hasAnyAuthority("HOST", "ROLE_HOST")

                        .requestMatchers(HttpMethod.GET, "/api/reservations/pending")
                        .hasAnyAuthority("HOST", "ROLE_HOST")
                        .requestMatchers(HttpMethod.POST, "/api/reservations/validate-ticket")
                        .hasAnyAuthority("HOST", "ROLE_HOST")
                        .requestMatchers(HttpMethod.GET, "/api/reservations/**")
                        .hasAnyAuthority("HOST", "ROLE_HOST")
                        .requestMatchers(HttpMethod.DELETE, "/api/reservations/**")
                        .hasAnyAuthority("HOST", "ROLE_HOST")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}