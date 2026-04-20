package tn.esprit.spring.baladna.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.user.entity.Role;
import tn.esprit.spring.baladna.user.entity.Status;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class OAuth2Service {
    private final UserRepository userRepo;
    private final JwtService jwtService;

    public String processOAuth2User(OAuth2User oAuth2User, String provider) {
        String email = oAuth2User.getAttribute("email");

        // ✅ fix email null pour Facebook
        if (email == null) {
            String id = oAuth2User.getAttribute("id");
            email = id + "@facebook.oauth";
        }

        // ✅ variables finales pour la lambda
        final String firstNameAttr = oAuth2User.getAttribute("given_name");
        final String lastNameAttr = oAuth2User.getAttribute("family_name");
        final String nameAttr = oAuth2User.getAttribute("name");
        final String finalEmail = email;

        final String firstName = firstNameAttr != null ? firstNameAttr :
                (nameAttr != null ? nameAttr.split(" ")[0] : "User");

        final String lastName = lastNameAttr != null ? lastNameAttr :
                (nameAttr != null && nameAttr.split(" ").length > 1
                        ? nameAttr.split(" ")[1] : provider);

        // ✅ utiliser finalEmail dans la lambda
        User user = userRepo.findByEmail(finalEmail).orElseGet(() -> {
            User newUser = User.builder()
                    .email(finalEmail)       // ✅ final
                    .firstName(firstName)   // ✅ final
                    .lastName(lastName)     // ✅ final
                    .password("OAUTH2_" + provider.toUpperCase())
                    .role(Role.TOURIST)
                    .status(Status.ACTIVE)
                    .preferredLanguage("FR")
                    .build();
            return userRepo.save(newUser);
        });
        return jwtService.generateToken(user);
    }
}