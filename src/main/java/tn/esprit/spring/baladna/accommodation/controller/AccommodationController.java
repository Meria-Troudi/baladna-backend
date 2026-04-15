package tn.esprit.spring.baladna.accommodation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.spring.baladna.accommodation.dto.*;
import tn.esprit.spring.baladna.accommodation.service.AccommodationBookingService;
import tn.esprit.spring.baladna.accommodation.service.AccommodationPricingService;
import tn.esprit.spring.baladna.accommodation.service.AccommodationService;
import tn.esprit.spring.baladna.accommodation.service.TripAiSuggestionService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accommodations")
@RequiredArgsConstructor
public class AccommodationController {

    private final AccommodationService accommodationService;
    private final AccommodationPricingService accommodationPricingService;
    private final AccommodationBookingService accommodationBookingService;
    private final TripAiSuggestionService tripAiSuggestionService;

    @GetMapping("/public/map")
    public List<AccommodationResponseDto> listForMap() {
        return accommodationService.listPublicForMap();
    }

    @GetMapping("/public/list")
    public List<AccommodationResponseDto> listAll() {
        return accommodationService.listAllPublic();
    }

    @GetMapping("/public/{id}")
    public AccommodationResponseDto getOne(@PathVariable UUID id) {
        return accommodationService.getPublic(id);
    }

    @GetMapping("/public/suggestions")
    public List<AccommodationResponseDto> suggestions(
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String intention,
            Authentication authentication) {
        String email = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName()
                : null;
        return accommodationService.suggestions(destination, intention, email);
    }

    @PostMapping("/public/quote")
    public QuoteResponseDto quote(@Valid @RequestBody QuoteRequestDto request) {
        return accommodationPricingService.quote(request);
    }

    @PostMapping("/public/trip-suggest")
    public TripSuggestionResponseDto tripSuggest(@Valid @RequestBody TripDescribeRequestDto request) {
        return tripAiSuggestionService.suggest(request.getDescription());
    }

    @GetMapping("/host/mine")
    @PreAuthorize("hasAnyRole('HOST','ADMIN')")
    public List<AccommodationResponseDto> mine(Authentication authentication) {
        return accommodationService.listMine(authentication.getName());
    }

    @GetMapping("/host/client-reviews")
    @PreAuthorize("hasAnyRole('HOST','ADMIN')")
    public List<ClientReviewResponseDto> clientReviews(Authentication authentication) {
        return accommodationBookingService.hostClientReviews(authentication.getName());
    }

    @GetMapping("/host/reservations")
    @PreAuthorize("hasAnyRole('HOST','ADMIN')")
    public List<HostReservationDto> hostReservations(Authentication authentication) {
        return accommodationBookingService.hostReservations(authentication.getName());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HOST','ADMIN')")
    public AccommodationResponseDto create(
            Authentication authentication,
            @Valid @RequestBody AccommodationRequestDto dto) {
        return accommodationService.create(authentication.getName(), dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HOST','ADMIN')")
    public AccommodationResponseDto update(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody AccommodationRequestDto dto) {
        return accommodationService.update(authentication.getName(), id, dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HOST','ADMIN')")
    public void delete(Authentication authentication, @PathVariable UUID id) {
        accommodationService.delete(authentication.getName(), id);
    }

    @PostMapping(value = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('HOST','ADMIN')")
    public void uploadCover(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file) {
        accommodationService.uploadCover(authentication.getName(), id, file);
    }
}
