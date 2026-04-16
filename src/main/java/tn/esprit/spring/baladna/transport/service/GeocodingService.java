package tn.esprit.spring.baladna.transport.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tn.esprit.spring.baladna.transport.dto.GeoLocationDTO;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeocodingService {

    private static final String NOMINATIM_SEARCH_URL = "https://nominatim.openstreetmap.org/search";
    private static final String NOMINATIM_REVERSE_URL = "https://nominatim.openstreetmap.org/reverse";
    private static final double TUNISIA_LEFT = 7.0;
    private static final double TUNISIA_TOP = 37.6;
    private static final double TUNISIA_RIGHT = 11.8;
    private static final double TUNISIA_BOTTOM = 30.0;
    private static final List<GeoLocationDTO> TUNISIA_LOCATIONS = List.of(
            tunisiaLocation("Utique", "Bizerte", 37.0553, 10.0444),
            tunisiaLocation("Sidi Bou Said", "Tunis", 36.8717, 10.3417),
            tunisiaLocation("La Marsa", "Tunis", 36.8782, 10.3247),
            tunisiaLocation("Lac 2", "Tunis", 36.8421, 10.2783),
            tunisiaLocation("Lac 1", "Tunis", 36.8309, 10.2463),
            tunisiaLocation("Beb Aliwa", "Tunis", 36.7899, 10.1738),
            tunisiaLocation("Tunis Marine", "Tunis", 36.8008, 10.1800),
            tunisiaLocation("Cite de la culture", "Tunis", 36.8067, 10.1815),
            tunisiaLocation("Ariana", "Ariana", 36.8663, 10.1647),
            tunisiaLocation("Ben Arous", "Ben Arous", 36.7531, 10.2189),
            tunisiaLocation("Bizerte", "Bizerte", 37.2744, 9.8739),
            tunisiaLocation("Sfax", "Sfax", 34.7406, 10.7603),
            tunisiaLocation("Sousse", "Sousse", 35.8256, 10.6411),
            tunisiaLocation("Nabeul", "Nabeul", 36.4561, 10.7376),
            tunisiaLocation("Hammamet", "Nabeul", 36.4000, 10.6167),
            tunisiaLocation("Monastir", "Monastir", 35.7770, 10.8262),
            tunisiaLocation("Mahdia", "Mahdia", 35.5047, 11.0622),
            tunisiaLocation("Kairouan", "Kairouan", 35.6781, 10.0963),
            tunisiaLocation("Gabes", "Gabes", 33.8815, 10.0982),
            tunisiaLocation("Medenine", "Medenine", 33.3549, 10.5055),
            tunisiaLocation("Djerba", "Medenine", 33.8076, 10.8451),
            tunisiaLocation("Tozeur", "Tozeur", 33.9197, 8.1335),
            tunisiaLocation("Kebili", "Kebili", 33.7044, 8.9690),
            tunisiaLocation("Gafsa", "Gafsa", 34.4250, 8.7842),
            tunisiaLocation("Zarzis", "Medenine", 33.5039, 11.1122),
            tunisiaLocation("Tabarka", "Jendouba", 36.9544, 8.7580)
    );

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<GeoLocationDTO> getKnownTunisiaLocations() {
        return TUNISIA_LOCATIONS.stream()
                .map(this::copyLocation)
                .collect(Collectors.toList());
    }

    public List<GeoLocationDTO> searchLocations(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.length() < 2) {
            return List.of();
        }

        List<GeoLocationDTO> localMatches = searchKnownTunisiaLocations(normalizedQuery);
        if (!localMatches.isEmpty()) {
            return localMatches;
        }

        for (String candidateQuery : buildCandidateQueries(normalizedQuery)) {
            String url = UriComponentsBuilder.fromUriString(NOMINATIM_SEARCH_URL)
                    .queryParam("q", candidateQuery)
                    .queryParam("format", "jsonv2")
                    .queryParam("addressdetails", 1)
                    .queryParam("countrycodes", "tn")
                    .queryParam("viewbox", TUNISIA_LEFT + "," + TUNISIA_TOP + "," + TUNISIA_RIGHT + "," + TUNISIA_BOTTOM)
                    .queryParam("bounded", 1)
                    .queryParam("limit", 5)
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();

            List<GeoLocationDTO> results = fetchSearchResults(url, candidateQuery);
            if (!results.isEmpty()) {
                return results;
            }
        }

        return List.of();
    }

    public GeoLocationDTO reverseGeocode(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }

        String url = UriComponentsBuilder.fromUriString(NOMINATIM_REVERSE_URL)
                .queryParam("lat", latitude)
                .queryParam("lon", longitude)
                .queryParam("format", "jsonv2")
                .queryParam("addressdetails", 1)
                .queryParam("zoom", 16)
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().isBlank()) {
                return null;
            }

            JsonNode item = objectMapper.readTree(response.getBody());
            if (!item.isObject()) {
                return findNearestKnownTunisiaLocation(latitude, longitude);
            }

            GeoLocationDTO result = toGeoLocation(item, latitude, longitude);
            return result != null ? result : findNearestKnownTunisiaLocation(latitude, longitude);
        } catch (Exception exception) {
            log.warn("Reverse geocoding failed for lat={}, lng={}: {}", latitude, longitude, exception.getMessage());
            return findNearestKnownTunisiaLocation(latitude, longitude);
        }
    }

    private List<GeoLocationDTO> fetchSearchResults(String url, String normalizedQuery) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().isBlank()) {
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            if (!root.isArray()) {
                return List.of();
            }

            List<GeoLocationDTO> results = new ArrayList<>();
            for (JsonNode item : root) {
                GeoLocationDTO geoLocation = toGeoLocation(item, null, null);
                if (geoLocation != null) {
                    results.add(geoLocation);
                }
            }

            return results;
        } catch (Exception exception) {
            log.warn("Geocoding failed for query '{}': {}", normalizedQuery, exception.getMessage());
            return List.of();
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "BaladnaTransport/1.0 (station geocoding)");
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, "fr,en");
        return headers;
    }

    private List<GeoLocationDTO> searchKnownTunisiaLocations(String query) {
        String normalizedQuery = normalizeText(query);

        return TUNISIA_LOCATIONS.stream()
                .filter(location -> {
                    String haystack = normalizeText(location.getName() + " " + location.getCity() + " " + location.getDisplayName());
                    return haystack.contains(normalizedQuery) || normalizedQuery.contains(normalizeText(location.getName()));
                })
                .sorted(Comparator.comparingInt((GeoLocationDTO location) -> scoreLocationMatch(location, normalizedQuery)).reversed())
                .limit(5)
                .map(this::copyLocation)
                .collect(Collectors.toList());
    }

    private List<String> buildCandidateQueries(String query) {
        Set<String> candidates = new LinkedHashSet<>();
        String trimmed = query.trim();
        candidates.add(trimmed);
        candidates.add(trimmed + ", Tunisie");
        candidates.add(trimmed + ", Tunisia");

        String withoutCountryAlias = trimmed
                .replaceAll("(?iu)\\b(tunisie|tunisia|tunisia\\b|tunisie\\b)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (!withoutCountryAlias.isBlank()) {
            candidates.add(withoutCountryAlias);
            candidates.add(withoutCountryAlias + ", Tunisie");
            candidates.add(withoutCountryAlias + ", Tunisia");
        }

        String lowerWithoutCountry = withoutCountryAlias.toLowerCase(Locale.ROOT);
        if (!lowerWithoutCountry.isBlank() && !lowerWithoutCountry.contains("tunis")) {
            candidates.add(withoutCountryAlias + " Tunis");
        }

        return new ArrayList<>(candidates);
    }

    private int scoreLocationMatch(GeoLocationDTO location, String normalizedQuery) {
        String normalizedName = normalizeText(location.getName());
        String normalizedCity = normalizeText(location.getCity());
        String normalizedDisplay = normalizeText(location.getDisplayName());

        int score = 0;
        if (normalizedName.equals(normalizedQuery)) {
            score += 100;
        }
        if (normalizedCity.equals(normalizedQuery)) {
            score += 90;
        }
        if (normalizedDisplay.contains(normalizedQuery)) {
            score += 50;
        }
        if (normalizedName.contains(normalizedQuery)) {
            score += 40;
        }
        if (normalizedCity.contains(normalizedQuery)) {
            score += 30;
        }
        return score;
    }

    private GeoLocationDTO toGeoLocation(JsonNode item, Double fallbackLatitude, Double fallbackLongitude) {
        String countryCode = item.path("address").path("country_code").asText("");
        if (!countryCode.isBlank() && !"tn".equalsIgnoreCase(countryCode)) {
            return null;
        }

        Double latitude = fallbackLatitude != null ? fallbackLatitude : parseDouble(item.path("lat").asText(null));
        Double longitude = fallbackLongitude != null ? fallbackLongitude : parseDouble(item.path("lon").asText(null));
        if (latitude == null || longitude == null) {
            return null;
        }

        String name = firstNonBlank(
                item.path("name").asText(null),
                item.path("address").path("suburb").asText(null),
                item.path("address").path("road").asText(null),
                item.path("display_name").asText(null)
        );
        String city = firstNonBlank(
                item.path("address").path("city").asText(null),
                item.path("address").path("town").asText(null),
                item.path("address").path("village").asText(null),
                item.path("address").path("municipality").asText(null),
                item.path("address").path("county").asText(null),
                item.path("address").path("state_district").asText(null),
                item.path("address").path("state").asText(null)
        );
        String displayName = item.path("display_name").asText(name);

        return GeoLocationDTO.builder()
                .name(name)
                .city(city)
                .displayName(displayName)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }

    private Double parseDouble(String value) {
        try {
            return value == null || value.isBlank() ? null : Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private GeoLocationDTO findNearestKnownTunisiaLocation(Double latitude, Double longitude) {
        return TUNISIA_LOCATIONS.stream()
                .min(Comparator.comparingDouble(location -> squaredDistance(latitude, longitude, location.getLatitude(), location.getLongitude())))
                .map(this::copyLocation)
                .orElse(null);
    }

    private double squaredDistance(double lat1, double lng1, double lat2, double lng2) {
        double latDiff = lat1 - lat2;
        double lngDiff = lng1 - lng2;
        return latDiff * latDiff + lngDiff * lngDiff;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value
                .toLowerCase(Locale.ROOT)
                .replace('é', 'e')
                .replace('è', 'e')
                .replace('ê', 'e')
                .replace('à', 'a')
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private GeoLocationDTO copyLocation(GeoLocationDTO location) {
        return GeoLocationDTO.builder()
                .name(location.getName())
                .city(location.getCity())
                .displayName(location.getDisplayName())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();
    }

    private static GeoLocationDTO tunisiaLocation(String name, String city, double latitude, double longitude) {
        return GeoLocationDTO.builder()
                .name(name)
                .city(city)
                .displayName(name + ", " + city + ", Tunisie")
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }
}
