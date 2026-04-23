package tn.esprit.spring.baladna.itinerary.util;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    /**
     * Extracts the currently authenticated user's Long id
     * from the security context.
     *
     * Primary path: JwtFilter stores userId via authToken.setDetails(userId)
     * after calling jwtService.extractUserId(token) — zero DB call needed.
     *
     * Fallback path: if details are not set (JwtFilter not yet updated),
     * we look up the user by email from the principal — one DB call.
     */
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        // Primary: userId stored directly in authentication details by JwtFilter
        // This works once JwtFilter calls authToken.setDetails(jwtService.extractUserId(token))
        Object details = auth.getDetails();
        if (details instanceof Long) {
            return (Long) details;
        }

        // Fallback: extract email from principal and look up user in DB
        // Works even before JwtFilter is updated — just costs one extra DB call per request
        String email = (String) auth.getPrincipal();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found in DB: " + email));

        return user.getId();
    }
}