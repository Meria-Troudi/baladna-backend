package tn.esprit.spring.baladna.accommodation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.accommodation.dto.*;
import tn.esprit.spring.baladna.accommodation.service.AccommodationBookingService;

import java.util.List;

@RestController
@RequestMapping("/api/accommodations/tourist")
@RequiredArgsConstructor
public class AccommodationTouristController {

    private final AccommodationBookingService bookingService;

    @PostMapping("/book")
    public BookingResponseDto book(Authentication authentication, @Valid @RequestBody BookingRequestDto body) {
        return bookingService.book(authentication.getName(), body);
    }

    @PostMapping("/pay")
    public PayResponseDto pay(Authentication authentication, @Valid @RequestBody PayRequestDto body) {
        return bookingService.pay(authentication.getName(), body);
    }

    @GetMapping("/reservations")
    public List<TouristReservationDto> myReservations(Authentication authentication) {
        return bookingService.myReservations(authentication.getName());
    }

    @PostMapping("/reviews")
    public void submitReview(Authentication authentication, @Valid @RequestBody ReviewRequestDto body) {
        bookingService.submitReview(authentication.getName(), body);
    }
}
