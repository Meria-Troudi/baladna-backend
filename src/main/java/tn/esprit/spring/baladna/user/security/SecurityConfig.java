package tn.esprit.spring.baladna.user.security; 

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.MediaType;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

        private final JwtFilter jwtFilter;
        private final OAuth2SuccessHandler oAuth2SuccessHandler;
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"message\":\"Unauthorized\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"message\":\"Access denied\"}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // ✅ Public event reservation endpoint
                        .requestMatchers("/api/events/event-reservation/confirmed-waitlisted").permitAll()
                        // ✅ Tout utilisateur connecté
                        .requestMatchers("/api/events/event-reservation/**","/api/forum/**").authenticated()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/events/**").permitAll()
                        .requestMatchers("/api/chat/**").permitAll()  // Chat endpoints - includes /info, WebSocket SockJS paths, and all transports. JWT validation happens in WebSocket handshake interceptor
                       
                        .requestMatchers("/api/itineraries/calendar/auth-url").authenticated()
                        .requestMatchers("/api/itineraries/calendar/oauth/callback").permitAll()  // OAuth callback from Google needs public access
                        .requestMatchers(HttpMethod.POST, "/api/itinerary/recommendations/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/itinerary/recommendations/similar/**").permitAll()
                        .requestMatchers("/api/itineraries/**").authenticated()  // All itinerary endpoints require authentication

                        .requestMatchers(HttpMethod.GET, "/api/users/{id}").authenticated()
                        .requestMatchers("/api/profile/**").authenticated()

                        .requestMatchers(
                                "/login/oauth2/**",
                                "/oauth2/**",
                                "/api/events/**",
                                "/api/rh/interviews",        // ✅ public
                                "/api/rh/interviews/*",      // ✅ public
                                "/api/rh/apply",
                                "/uploads/**",
                                "/uploads/photos/**"

                        ).permitAll()

                        .requestMatchers(
                                "/api/dashboard-admin/**",
                                "/api/rh/admin/**").hasRole("ADMIN")

                        //transport
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                         .requestMatchers(HttpMethod.GET, "/api/stations/**").hasAnyRole("HOST", "TOURIST")
                        .requestMatchers(HttpMethod.POST, "/api/stations/**").hasRole("HOST")
                        .requestMatchers(HttpMethod.PUT, "/api/stations/**").hasRole("HOST")
                        .requestMatchers(HttpMethod.DELETE, "/api/stations/**").hasRole("HOST")

                        .requestMatchers(HttpMethod.GET, "/api/trajets/**").hasAnyRole("HOST", "TOURIST")
                        .requestMatchers(HttpMethod.POST, "/api/trajets/**").hasRole("HOST")
                        .requestMatchers(HttpMethod.PUT, "/api/trajets/**").hasRole("HOST")
                        .requestMatchers(HttpMethod.DELETE, "/api/trajets/**").hasRole("HOST")

                        .requestMatchers(HttpMethod.GET, "/api/transports/ai/report").hasRole("HOST")
                        .requestMatchers(HttpMethod.GET, "/api/transports/ai/alerts").hasRole("HOST")
                        .requestMatchers(HttpMethod.GET, "/api/transports/ai/model/summary").hasRole("HOST")
                        .requestMatchers(HttpMethod.POST, "/api/transports/ai/model/train").hasRole("HOST")
                        .requestMatchers(HttpMethod.POST, "/api/transports/ai/dataset/**").hasRole("HOST")
                        .requestMatchers(HttpMethod.GET, "/api/transports/ai/dataset/**").hasRole("HOST")
                        .requestMatchers(HttpMethod.GET, "/api/transports/ai/delay-prediction/**").hasAnyRole("HOST", "TOURIST")
                        .requestMatchers(HttpMethod.GET, "/api/transports/ai/recommendation").authenticated()

                        .requestMatchers(HttpMethod.GET, "/api/transports/**").hasAnyRole("HOST", "TOURIST")
                        .requestMatchers(HttpMethod.POST, "/api/transports/**").hasRole("HOST")
                        .requestMatchers(HttpMethod.PUT, "/api/transports/**").hasRole("HOST")
                        .requestMatchers(HttpMethod.DELETE, "/api/transports/**").hasRole("HOST")

                        .requestMatchers(HttpMethod.GET, "/api/reservations").hasAnyRole("HOST", "TOURIST")
                        .requestMatchers(HttpMethod.GET, "/api/reservations/me").hasRole("TOURIST")
                        .requestMatchers(HttpMethod.POST, "/api/reservations").hasRole("TOURIST")
                        .requestMatchers(HttpMethod.PUT, "/api/reservations/*/cancel").hasRole("TOURIST")
                        .requestMatchers(HttpMethod.PUT, "/api/reservations/*/approve").hasRole("HOST")
                        .requestMatchers(HttpMethod.PUT, "/api/reservations/*/reject").hasRole("HOST")
                        .requestMatchers(HttpMethod.PUT, "/api/reservations/*/board").hasRole("HOST")
                        .requestMatchers(HttpMethod.GET, "/api/reservations/pending").hasRole("HOST")
                        .requestMatchers(HttpMethod.POST, "/api/reservations/validate-ticket").hasRole("HOST")
                        .requestMatchers(HttpMethod.GET, "/api/reservations/**").hasRole("HOST")
                        .requestMatchers(HttpMethod.DELETE, "/api/reservations/**").hasAnyRole("HOST", "TOURIST")
                        

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
                                "/api/marketplace/public/**",
                                // 🔽 AJOUTS IMPORTANTS pour le marketplace sans /api
                                "/api/ai/**",                    // ← AJOUTE CECI
                                "/products/**",
                                "/categories/**",
                                "/reviews/**"
                        ).permitAll()
                        .requestMatchers("/api/marketplace/admin/**" ).hasRole("ADMIN")
                        .requestMatchers(  "/api/marketplace/host/**").hasRole("HOST")
                        .requestMatchers( "/api/artisan/**", "/api/marketplace/artisan/**" ).hasRole("ARTISAN")

                        .requestMatchers( "/api/marketplace/tourist/**").hasRole("TOURIST")
                        .requestMatchers("/cart/**","/orders/**","/api/factures/**",  "/favorites/**" ).authenticated()
                        // ✅ MARKETPLACE (routes générales avec /api)
                        .requestMatchers(
                                "/api/products/**",
                                "/api/categories/**",
                                "/api/reviews/**"
                        ).permitAll()
                        // ✅ Itinerary AI Recommendations
                      .requestMatchers("/api/itinerary/recommendations/search").permitAll()
                        .requestMatchers("/api/itinerary/recommendations/similar/**").permitAll()
                        .requestMatchers("/api/itinerary/recommendations/train/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )

                
        
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler)
                );
                

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS","PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}