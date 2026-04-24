package tn.esprit.spring.baladna.user.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.net.URI;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource)
            throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // ✅ Public
                        .requestMatchers(
                                "/api/auth/**",
                                "/login/oauth2/**",
                                "/oauth2/**",
                                "/api/events/**",
                                "/api/accommodations/public/**",
                                "/uploads/**"
                        ).permitAll()

                        // ✅ ADMIN
                        .requestMatchers(
                                "/api/users/**",
                                "/api/dashboard-admin/**"
                        ).hasRole("ADMIN")

                        // ✅ HOST
                        .requestMatchers(
                                "/api/create-event/**"
                        ).hasRole("HOST")

                        .requestMatchers(HttpMethod.GET, "/api/accommodations/host/**").hasAnyRole("HOST", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/accommodations").hasAnyRole("HOST", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/accommodations/*/cover").hasAnyRole("HOST", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/accommodations/*").hasAnyRole("HOST", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/accommodations/*").hasAnyRole("HOST", "ADMIN")

                        // ✅ ARTISAN
                        .requestMatchers(
                                "/api/artisan/**"
                        ).hasRole("ARTISAN")

                        // Itinéraire / Google Calendar (callback public)
                        .requestMatchers("/api/itineraries/calendar/oauth/callback").permitAll()
                        .requestMatchers("/api/itineraries/calendar/auth-url").authenticated()
                        .requestMatchers("/api/itineraries/calendar/**").authenticated()

                        .requestMatchers(HttpMethod.GET, "/api/users/{id}").authenticated()

                        // Forum événements
                        .requestMatchers("/api/forum/**").authenticated()

                        // ✅ Tout utilisateur connecté
                        .requestMatchers("/api/profile/**").authenticated()

                        .anyRequest().authenticated()
                )

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return request -> {
            String origin = request.getHeader(HttpHeaders.ORIGIN);
            if (origin == null || origin.isBlank() || !isDevLocalOrigin(origin)) {
                return null;
            }
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(List.of(origin));
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(List.of("*"));
            config.setAllowCredentials(true);
            config.setMaxAge(3600L);
            return config;
        };
    }

    private static boolean isDevLocalOrigin(String origin) {
        try {
            URI uri = URI.create(origin);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (host == null) {
                return false;
            }
            boolean localHost = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
            boolean httpish = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            return localHost && httpish;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}