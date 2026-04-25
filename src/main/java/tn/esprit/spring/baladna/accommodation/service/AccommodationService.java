package tn.esprit.spring.baladna.accommodation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.baladna.accommodation.dto.*;
import tn.esprit.spring.baladna.accommodation.entity.Accommodation;
import tn.esprit.spring.baladna.accommodation.entity.AccommodationReservation;
import tn.esprit.spring.baladna.accommodation.entity.Room;
import tn.esprit.spring.baladna.accommodation.entity.enums.AccommodationReservationStatus;
import tn.esprit.spring.baladna.accommodation.entity.enums.AccommodationStatus;
import tn.esprit.spring.baladna.accommodation.entity.enums.AccommodationType;
import tn.esprit.spring.baladna.accommodation.repository.AccommodationRepository;
import tn.esprit.spring.baladna.accommodation.repository.AccommodationReservationRepository;
import tn.esprit.spring.baladna.accommodation.repository.AccommodationReviewRepository;
import tn.esprit.spring.baladna.accommodation.repository.RoomAvailabilityRepository;
import tn.esprit.spring.baladna.accommodation.repository.RoomRepository;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccommodationService {

    /** All host listings shown to tourists (drafts included so new places appear before “Live”). */
    private static final List<AccommodationStatus> PUBLIC_BROWSE_STATUSES = List.of(
            AccommodationStatus.ACTIVE,
            AccommodationStatus.INACTIVE,
            AccommodationStatus.DRAFT
    );

    @Value("${app.accommodation.upload-dir:uploads/accommodations}")
    private String uploadDir;

    @Value("${app.accommodation.public-base-url:http://localhost:8081}")
    private String publicBaseUrl;

    private final AccommodationRepository accommodationRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final AccommodationReservationRepository reservationRepository;
    private final RoomAvailabilityRepository roomAvailabilityRepository;
    private final AccommodationReviewRepository accommodationReviewRepository;

    @Transactional(readOnly = true)
    public List<AccommodationResponseDto> listPublicForMap() {
        return accommodationRepository.findByStatusInWithRooms(PUBLIC_BROWSE_STATUSES).stream()
                .map(a -> toDto(a, true))
                .filter(dto -> dto.getLatitude() != null && dto.getLongitude() != null)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AccommodationResponseDto> listAllPublic() {
        return accommodationRepository.findByStatusInWithRooms(PUBLIC_BROWSE_STATUSES).stream()
                .map(a -> toDto(a, true))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AccommodationResponseDto getPublic(UUID id) {
        Accommodation a = accommodationRepository.findByIdWithRooms(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!PUBLIC_BROWSE_STATUSES.contains(a.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return toDto(a, true);
    }

    @Transactional(readOnly = true)
    public List<AccommodationResponseDto> listMine(String email) {
        User host = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return accommodationRepository.findByHostIdWithRooms(host.getId()).stream()
                .map(a -> toDto(a, true))
                .collect(Collectors.toList());
    }

    @Transactional
    public AccommodationResponseDto create(String email, AccommodationRequestDto dto) {
        User host = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        UUID accId = UUID.randomUUID();
        Accommodation acc = Accommodation.builder()
                .id(accId)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .address(dto.getAddress())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .maxGuests(dto.getMaxGuests())
                .amenities(dto.getAmenities())
                .rules(dto.getRules())
                .type(dto.getType())
                .status(dto.getStatus() != null ? dto.getStatus() : AccommodationStatus.ACTIVE)
                .hostId(host.getId())
                .build();

        for (RoomRequestDto rDto : dto.getRooms()) {
            Room room = Room.builder()
                    .id(UUID.randomUUID())
                    .accommodation(acc)
                    .type(rDto.getType())
                    .capacity(rDto.getCapacity())
                    .pricePerNight(rDto.getPricePerNight())
                    .amenities(rDto.getAmenities())
                    .build();
            acc.getRooms().add(room);
        }

        accommodationRepository.save(acc);
        return toDto(acc, true);
    }

    @Transactional
    public AccommodationResponseDto update(String email, UUID id, AccommodationRequestDto dto) {
        User host = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Accommodation acc = accommodationRepository.findByIdAndHostIdWithRooms(id, host.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        acc.setTitle(dto.getTitle());
        acc.setDescription(dto.getDescription());
        acc.setAddress(dto.getAddress());
        acc.setLatitude(dto.getLatitude());
        acc.setLongitude(dto.getLongitude());
        acc.setMaxGuests(dto.getMaxGuests());
        acc.setAmenities(dto.getAmenities());
        acc.setRules(dto.getRules());
        acc.setType(dto.getType());
        if (dto.getStatus() != null) {
            acc.setStatus(dto.getStatus());
        }

        acc.getRooms().clear();
        for (RoomRequestDto rDto : dto.getRooms()) {
            UUID roomId = rDto.getId() != null && !rDto.getId().isBlank()
                    ? UUID.fromString(rDto.getId())
                    : UUID.randomUUID();
            Room room = Room.builder()
                    .id(roomId)
                    .accommodation(acc)
                    .type(rDto.getType())
                    .capacity(rDto.getCapacity())
                    .pricePerNight(rDto.getPricePerNight())
                    .amenities(rDto.getAmenities())
                    .build();
            acc.getRooms().add(room);
        }

        accommodationRepository.save(acc);
        return toDto(acc, true);
    }

    @Transactional
    public void uploadCover(String email, UUID id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File required");
        }
        User host = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Accommodation acc = accommodationRepository.findByIdAndHostIdWithRooms(id, host.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String original = Optional.ofNullable(file.getOriginalFilename()).orElse("cover");
        String safe = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        String name = acc.getId() + "_" + System.currentTimeMillis() + "_" + safe;

        try {
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);
            Path target = dir.resolve(name);
            Files.copy(file.getInputStream(), target);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store file");
        }

        acc.setCoverImageFileName(name);
        accommodationRepository.save(acc);
    }

    @Transactional
    public void delete(String email, UUID id) {
        User host = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Accommodation acc = accommodationRepository.findByIdAndHostIdWithRooms(id, host.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        roomAvailabilityRepository.deleteByAccommodationId(id);
        accommodationReviewRepository.deleteByAccommodationId(id);
        reservationRepository.deleteAllByAccommodationId(id);

        if (acc.getCoverImageFileName() != null && !acc.getCoverImageFileName().isBlank()) {
            try {
                Files.deleteIfExists(Paths.get(uploadDir).resolve(acc.getCoverImageFileName()));
            } catch (IOException ignored) {
                // file missing or not deletable — still remove DB row
            }
        }

        accommodationRepository.delete(acc);
    }

    @Transactional(readOnly = true)
    public List<AccommodationResponseDto> suggestions(String destination, String intention, String email) {
        String destNorm = destination == null ? "" : destination.trim().toLowerCase(Locale.ROOT);
        String intentNorm = intention == null ? "" : intention.trim().toLowerCase(Locale.ROOT);

        Set<AccommodationType> preferredTypes = new HashSet<>();
        if (email != null && !email.isBlank()) {
            userRepository.findByEmail(email).ifPresent(user -> {
                for (AccommodationReservation r : reservationRepository.findByUserIdForSuggestions(user.getId())) {
                    if (r.getStatus() != AccommodationReservationStatus.CANCELLED) {
                        preferredTypes.add(r.getAccommodation().getType());
                    }
                }
            });
        }

        List<Accommodation> all = accommodationRepository.findByStatusInWithRooms(PUBLIC_BROWSE_STATUSES);
        record Scored(Accommodation a, int score) {}
        List<Scored> scored = new ArrayList<>();
        for (Accommodation a : all) {
            int score = 1;
            String hay = (a.getTitle() + " " + a.getAddress() + " " + Optional.ofNullable(a.getDescription()).orElse("")
                    + " " + Optional.ofNullable(a.getAmenities()).orElse("")).toLowerCase(Locale.ROOT);
            if (!destNorm.isEmpty() && hay.contains(destNorm)) {
                score += 5;
            }
            if (preferredTypes.contains(a.getType())) {
                score += 3;
            }
            if (!intentNorm.isEmpty()) {
                for (String token : intentNorm.split("[,\\s]+")) {
                    if (token.length() > 2 && hay.contains(token)) {
                        score += 2;
                        break;
                    }
                }
            }
            scored.add(new Scored(a, score));
        }

        scored.sort(Comparator.comparingInt(Scored::score).reversed());
        return scored.stream().limit(12).map(s -> toDto(s.a, true)).collect(Collectors.toList());
    }

    public AccommodationResponseDto toDto(Accommodation a, boolean includeRooms) {
        List<RoomResponseDto> rooms = includeRooms
                ? a.getRooms().stream()
                .sorted(Comparator.comparing(Room::getPricePerNight))
                .map(r -> RoomResponseDto.builder()
                        .id(r.getId().toString())
                        .type(r.getType())
                        .capacity(r.getCapacity())
                        .pricePerNight(r.getPricePerNight())
                        .amenities(r.getAmenities())
                        .build())
                .collect(Collectors.toList())
                : List.of();

        BigDecimal from = a.getRooms().isEmpty()
                ? null
                : a.getRooms().stream().map(Room::getPricePerNight).min(BigDecimal::compareTo).orElse(null);

        String coverUrl = null;
        if (a.getCoverImageFileName() != null && !a.getCoverImageFileName().isBlank()) {
            coverUrl = publicBaseUrl + "/uploads/accommodations/" + a.getCoverImageFileName();
        }

        return AccommodationResponseDto.builder()
                .id(a.getId().toString())
                .title(a.getTitle())
                .description(a.getDescription())
                .address(a.getAddress())
                .latitude(a.getLatitude())
                .longitude(a.getLongitude())
                .maxGuests(a.getMaxGuests())
                .amenities(a.getAmenities())
                .rules(a.getRules())
                .type(a.getType())
                .status(a.getStatus())
                .hostId(a.getHostId())
                .coverImageUrl(coverUrl)
                .rooms(rooms)
                .fromPricePerNight(from)
                .build();
    }
}
