package tn.esprit.spring.baladna.user.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

                        // ✅ Public (tout le monde peut accéder)
                        .requestMatchers(
                                "/api/auth/**",
                                "/login/oauth2/**",
                                "/oauth2/**",
                                "/api/events/**",
                                "/api/marketplace/public/**",
                                // 🔽 AJOUTS IMPORTANTS pour le marketplace sans /api
                                "/api/ai/**",                    // ← AJOUTE CECI
                                "/products/**",
                                "/categories/**",
                                "/reviews/**"
                        ).permitAll()

                        // ✅ ADMIN
                        .requestMatchers(
                                "/api/users/**",
                                "/api/dashboard-admin/**",
                                "/api/marketplace/admin/**"
                        ).hasRole("ADMIN")

                        // ✅ HOST
                        .requestMatchers(
                                "/api/create-event/**",
                                "/api/create-accommodation/**",
                                "/api/marketplace/host/**"
                        ).hasRole("HOST")

                        // ✅ ARTISAN
                        .requestMatchers(
                                "/api/artisan/**",
                                "/api/marketplace/artisan/**"
                        ).hasRole("ARTISAN")

                        // ✅ TOURIST
                        .requestMatchers(
                                "/api/marketplace/tourist/**"
                        ).hasRole("TOURIST")

                        // ✅ Tout utilisateur connecté
                        .requestMatchers("/api/profile/**").authenticated()
                        /// ///////////////////////


                        .requestMatchers(
                                "/cart/**",
                                "/orders/**",
                                "/api/factures/**",             // ← AJOUTE CECI aussi
                                "/favorites/**"
                        ).authenticated()
/// /////////////////////////////////////////

                        // ✅ MARKETPLACE (routes générales avec /api)
                        .requestMatchers(
                                "/api/products/**",
                                "/api/categories/**",
                                "/api/reviews/**"
                        ).permitAll()

                        .anyRequest().authenticated()
                )

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}