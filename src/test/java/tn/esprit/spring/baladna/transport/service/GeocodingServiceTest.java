package tn.esprit.spring.baladna.transport.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import tn.esprit.spring.baladna.transport.dto.GeoLocationDTO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeocodingServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private GeocodingService geocodingService;

    @BeforeEach
    void setUp() {
        geocodingService = new GeocodingService(restTemplate);
    }

    @Test
    void shouldReturnKnownTunisiaLocationWithoutCallingExternalService() {
        List<GeoLocationDTO> results = geocodingService.searchLocations("Utique");

        assertFalse(results.isEmpty());
        assertEquals("Utique", results.get(0).getName());
        assertEquals("Bizerte", results.get(0).getCity());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void shouldFallbackToNearestKnownTunisiaLocationWhenReverseGeocodingFails() {
        when(restTemplate.exchange(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.<Class<String>>any()
        )).thenThrow(new RuntimeException("network down"));

        GeoLocationDTO result = geocodingService.reverseGeocode(36.8717, 10.3417);

        assertNotNull(result);
        assertEquals("Sidi Bou Said", result.getName());
        assertEquals("Tunis", result.getCity());
    }
}
