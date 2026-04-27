package tn.esprit.spring.baladna.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;
import tn.esprit.spring.baladna.user.service.JwtService;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepo;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/auth")
        
                || path.startsWith("/products")
                || path.startsWith("/categories")
                || path.startsWith("/favorites")
               // || path.startsWith("/orders")
                || path.startsWith("/reviews");




    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        log.info("Request to: {} - Auth header: {}", request.getRequestURI(), authHeader != null ? "present" : "null");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("No Bearer token found");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            log.info("Token extracted (length: {})", token.length());

            String email = jwtService.extractEmail(token);
            log.info("Email extracted from token: {}", email);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userRepo.findByEmail(email).orElseThrow();
                log.info("User found: {} with role: {}", user.getEmail(), user.getRole());

                String role = user.getRole().name();
                log.info("Setting authentication with role: ROLE_{}", role);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                user.getEmail(),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.info("Authentication set successfully");
            }
        } catch (Exception e) {
            log.error("JWT Authentication failed: {}", e.getMessage());
            log.error("Full exception: ", e);
        }

        filterChain.doFilter(request, response);
    }
}