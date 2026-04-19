package tn.esprit.spring.baladna.itinerary.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Google OAuth 2.0 Token Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleTokenResponse {
    
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("refresh_token")
    private String refreshToken;
    
    @JsonProperty("expires_in")
    private Long expiresIn;
    
    @JsonProperty("token_type")
    private String tokenType;
    
    @JsonProperty("scope")
    private String scope;
}
