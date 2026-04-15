package tn.esprit.spring.baladna.accommodation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.baladna.accommodation.dto.QuoteRequestDto;
import tn.esprit.spring.baladna.accommodation.dto.QuoteResponseDto;
import tn.esprit.spring.baladna.accommodation.entity.Accommodation;
import tn.esprit.spring.baladna.accommodation.entity.Room;
import tn.esprit.spring.baladna.accommodation.entity.enums.AccommodationReservationStatus;
import tn.esprit.spring.baladna.accommodation.entity.enums.AccommodationStatus;
import tn.esprit.spring.baladna.accommodation.repository.AccommodationRepository;
import tn.esprit.spring.baladna.accommodation.repository.AccommodationReservationRepository;
import tn.esprit.spring.baladna.accommodation.repository.RoomRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AccommodationPricingService {

    private static final BigDecimal LAST_ROOM_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal FULL_PROPERTY_MULTIPLIER = new BigDecimal("0.80");

    private final AccommodationRepository accommodationRepository;
    private final RoomRepository roomRepository;
    private final AccommodationReservationRepository reservationRepository;

    public QuoteResponseDto quote(QuoteRequestDto request) {
        if (!request.getCheckOut().isAfter(request.getCheckIn())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "checkOut must be after checkIn");
        }

        Accommodation accommodation = accommodationRepository.findById(request.getAccommodationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Accommodation not found"));
        if (accommodation.getStatus() != AccommodationStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This accommodation is not open for booking");
        }

        LocalDateTime start = request.getCheckIn().atStartOfDay();
        LocalDateTime end = request.getCheckOut().atStartOfDay();

        int nights = (int) ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut());
        List<Room> allRooms = roomRepository.findByAccommodation_IdOrderByPricePerNightAsc(accommodation.getId());
        if (allRooms.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No rooms configured for this accommodation");
        }

        Set<UUID> blockedRoomIds = computeBlockedRoomIds(accommodation.getId(), start, end, allRooms);
        long availableCount = allRooms.stream().filter(r -> !blockedRoomIds.contains(r.getId())).count();

        Set<UUID> selectedIds = new HashSet<>(request.getRoomIds());
        if (selectedIds.size() != request.getRoomIds().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate room selection");
        }

        Map<UUID, Room> roomById = new HashMap<>();
        for (Room r : allRooms) {
            roomById.put(r.getId(), r);
        }

        for (UUID id : selectedIds) {
            if (!roomById.containsKey(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room does not belong to this accommodation");
            }
            if (blockedRoomIds.contains(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "One or more rooms are not available for these dates");
            }
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (UUID id : selectedIds) {
            Room room = roomById.get(id);
            subtotal = subtotal.add(room.getPricePerNight().multiply(BigDecimal.valueOf(nights)));
        }

        int totalRooms = allRooms.size();
        boolean fullProperty = selectedIds.size() == totalRooms;
        boolean lastRoomOnly = selectedIds.size() == 1 && availableCount == 1;

        List<String> appliedRules = new ArrayList<>();
        BigDecimal total = subtotal;
        BigDecimal lastPremiumPct = null;
        BigDecimal fullDiscPct = null;

        if (fullProperty) {
            total = total.multiply(FULL_PROPERTY_MULTIPLIER).setScale(2, RoundingMode.HALF_UP);
            fullDiscPct = new BigDecimal("20");
            appliedRules.add("20% discount applied: entire property reserved");
        } else if (lastRoomOnly) {
            total = total.multiply(LAST_ROOM_MULTIPLIER).setScale(2, RoundingMode.HALF_UP);
            lastPremiumPct = new BigDecimal("20");
            appliedRules.add("20% premium applied: last available room");
        }

        return QuoteResponseDto.builder()
                .nights(nights)
                .subtotalBeforeAdjustments(subtotal.setScale(2, RoundingMode.HALF_UP))
                .lastRoomPremiumPercent(lastPremiumPct)
                .fullPropertyDiscountPercent(fullDiscPct)
                .total(total)
                .appliedRules(appliedRules)
                .build();
    }

    private Set<UUID> computeBlockedRoomIds(UUID accommodationId, LocalDateTime start, LocalDateTime end, List<Room> allRooms) {
        List<AccommodationReservationStatus> statuses = List.of(
                AccommodationReservationStatus.CONFIRMED,
                AccommodationReservationStatus.PENDING
        );
        var overlapping = reservationRepository.findOverlapping(accommodationId, start, end, statuses);
        Set<UUID> blocked = new HashSet<>();
        for (var res : overlapping) {
            if (res.getAssignedRoom() == null) {
                for (Room room : allRooms) {
                    blocked.add(room.getId());
                }
            } else {
                blocked.add(res.getAssignedRoom().getId());
            }
        }
        return blocked;
    }
}
