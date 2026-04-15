package tn.esprit.spring.baladna.accommodation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.baladna.accommodation.dto.*;
import tn.esprit.spring.baladna.accommodation.entity.*;
import tn.esprit.spring.baladna.accommodation.entity.enums.AccommodationReservationStatus;
import tn.esprit.spring.baladna.accommodation.entity.enums.PaymentStatus;
import tn.esprit.spring.baladna.accommodation.repository.*;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AccommodationBookingService {

    private final UserRepository userRepository;
    private final AccommodationRepository accommodationRepository;
    private final RoomRepository roomRepository;
    private final AccommodationReservationRepository reservationRepository;
    private final RoomAvailabilityRepository roomAvailabilityRepository;
    private final AccommodationReviewRepository reviewRepository;
    private final AccommodationPricingService pricingService;
    private final QrCodeService qrCodeService;
    private final BookingMailService bookingMailService;

    @Value("${app.accommodation.public-base-url:http://localhost:8081}")
    private String publicBaseUrl;

    @Transactional
    public BookingResponseDto book(String email, BookingRequestDto req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        QuoteRequestDto quoteReq = new QuoteRequestDto();
        quoteReq.setAccommodationId(req.getAccommodationId());
        quoteReq.setCheckIn(req.getCheckIn());
        quoteReq.setCheckOut(req.getCheckOut());
        quoteReq.setRoomIds(req.getRoomIds());
        QuoteResponseDto quote = pricingService.quote(quoteReq);

        Accommodation acc = accommodationRepository.findById(req.getAccommodationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Accommodation not found"));

        int nights = (int) ChronoUnit.DAYS.between(req.getCheckIn(), req.getCheckOut());
        List<UUID> roomIds = new ArrayList<>(req.getRoomIds());
        Map<UUID, Room> roomById = new HashMap<>();
        BigDecimal sumLines = BigDecimal.ZERO;
        Map<UUID, BigDecimal> lineByRoom = new LinkedHashMap<>();
        for (UUID id : roomIds) {
            Room room = roomRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid room"));
            if (!room.getAccommodation().getId().equals(acc.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room does not belong to this place");
            }
            roomById.put(id, room);
            BigDecimal line = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
            lineByRoom.put(id, line);
            sumLines = sumLines.add(line);
        }
        if (sumLines.compareTo(BigDecimal.ZERO) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pricing");
        }

        BigDecimal total = quote.getTotal();
        List<BigDecimal> shares = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < roomIds.size(); i++) {
            UUID id = roomIds.get(i);
            BigDecimal share;
            if (i < roomIds.size() - 1) {
                share = lineByRoom.get(id)
                        .divide(sumLines, 10, RoundingMode.HALF_UP)
                        .multiply(total)
                        .setScale(2, RoundingMode.HALF_UP);
                allocated = allocated.add(share);
            } else {
                share = total.subtract(allocated);
            }
            shares.add(share);
        }

        UUID groupId = UUID.randomUUID();
        String invoice = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        String confirmation = randomAlphanumeric(10);

        LocalDateTime checkInDt = req.getCheckIn().atStartOfDay();
        LocalDateTime checkOutDt = req.getCheckOut().atStartOfDay();

        List<UUID> savedIds = new ArrayList<>();
        for (int i = 0; i < roomIds.size(); i++) {
            UUID rid = roomIds.get(i);
            AccommodationReservation row = AccommodationReservation.builder()
                    .id(UUID.randomUUID())
                    .accommodation(acc)
                    .assignedRoom(roomById.get(rid))
                    .userId(user.getId())
                    .checkIn(checkInDt)
                    .checkOut(checkOutDt)
                    .guests(req.getGuests())
                    .priceTotal(shares.get(i))
                    .status(AccommodationReservationStatus.PENDING)
                    .paymentStatus(PaymentStatus.PENDING)
                    .confirmationCode(confirmation)
                    .invoiceNumber(invoice)
                    .bookingGroupId(groupId)
                    .build();
            reservationRepository.save(row);
            savedIds.add(row.getId());
        }

        return BookingResponseDto.builder()
                .bookingGroupId(groupId)
                .invoiceNumber(invoice)
                .confirmationCode(confirmation)
                .totalAmount(total)
                .reservationIds(savedIds)
                .paymentPending(true)
                .message("Réservation enregistrée. Procédez au paiement pour confirmer.")
                .build();
    }

    @Transactional
    public PayResponseDto pay(String email, PayRequestDto req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        List<AccommodationReservation> rows = reservationRepository.findByBookingGroupIdAndUserId(
                req.getBookingGroupId(), user.getId());
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }
        if (rows.get(0).getPaymentStatus() == PaymentStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already paid");
        }

        String digits = req.getCardNumber().replaceAll("\\D", "");
        if (digits.length() < 16) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid card number (demo: 16 digits)");
        }

        String invoice = rows.get(0).getInvoiceNumber();
        String confirmation = rows.get(0).getConfirmationCode();

        for (AccommodationReservation r : rows) {
            r.setStatus(AccommodationReservationStatus.CONFIRMED);
            r.setPaymentStatus(PaymentStatus.PAID);
            reservationRepository.save(r);
            fillRoomAvailability(r);
        }

        String qrPayload = "BALADNA|" + invoice + "|" + confirmation + "|" + user.getEmail();
        String qrDataUrl = qrCodeService.toPngDataUrl(qrPayload);

        String guestName = (user.getFirstName() != null ? user.getFirstName() : "") + " "
                + (user.getLastName() != null ? user.getLastName() : "");
        boolean emailed = bookingMailService.sendBookingConfirmation(
                user.getEmail(), guestName.trim(), invoice, confirmation, qrDataUrl);

        return PayResponseDto.builder()
                .invoiceNumber(invoice)
                .confirmationCode(confirmation)
                .qrImageBase64(qrDataUrl)
                .confirmationEmailSent(emailed)
                .message(emailed
                        ? "Paiement confirmé. Un e-mail avec le code QR a été envoyé."
                        : "Paiement confirmé. E-mail non configuré : utilisez le QR affiché à l’écran.")
                .build();
    }

    private void fillRoomAvailability(AccommodationReservation res) {
        Room room = res.getAssignedRoom();
        if (room == null) {
            return;
        }
        LocalDate d = res.getCheckIn().toLocalDate();
        LocalDate end = res.getCheckOut().toLocalDate();
        while (d.isBefore(end)) {
            RoomAvailability slot = RoomAvailability.builder()
                    .id(UUID.randomUUID())
                    .room(room)
                    .night(d)
                    .reservation(res)
                    .build();
            roomAvailabilityRepository.save(slot);
            d = d.plusDays(1);
        }
    }

    @Transactional(readOnly = true)
    public List<TouristReservationDto> myReservations(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        List<AccommodationReservation> list = reservationRepository.findMineDetailed(user.getId());
        List<TouristReservationDto> out = new ArrayList<>();
        for (AccommodationReservation r : list) {
            Accommodation acc = r.getAccommodation();
            String cover = null;
            if (acc.getCoverImageFileName() != null && !acc.getCoverImageFileName().isBlank()) {
                cover = publicBaseUrl + "/uploads/accommodations/" + acc.getCoverImageFileName();
            }
            boolean paid = r.getPaymentStatus() == PaymentStatus.PAID
                    || (r.getPaymentStatus() == null && r.getStatus() == AccommodationReservationStatus.CONFIRMED);
            Optional<AccommodationReview> existing = reviewRepository.findByUserIdAndReservationId(user.getId(), r.getId());
            boolean stayEnded = r.getCheckOut().toLocalDate().isBefore(LocalDate.now());
            boolean canReview = paid && r.getStatus() == AccommodationReservationStatus.CONFIRMED && stayEnded && existing.isEmpty();

            String roomSummary = r.getAssignedRoom() != null ? r.getAssignedRoom().getType().name() : "—";
            String payLabel = r.getPaymentStatus() != null ? r.getPaymentStatus().name()
                    : (paid ? "PAID" : "—");

            out.add(TouristReservationDto.builder()
                    .reservationId(r.getId())
                    .bookingGroupId(r.getBookingGroupId())
                    .accommodationId(acc.getId())
                    .accommodationTitle(acc.getTitle())
                    .coverImageUrl(cover)
                    .checkIn(r.getCheckIn().toLocalDate())
                    .checkOut(r.getCheckOut().toLocalDate())
                    .status(r.getStatus().name())
                    .paymentStatus(payLabel)
                    .invoiceNumber(r.getInvoiceNumber())
                    .confirmationCode(r.getConfirmationCode())
                    .priceTotal(r.getPriceTotal())
                    .roomSummary(roomSummary)
                    .canSubmitReview(canReview)
                    .existingReviewStars(existing.map(AccommodationReview::getStars).orElse(null))
                    .existingReviewComment(existing.map(AccommodationReview::getComment).orElse(null))
                    .build());
        }
        return out;
    }

    @Transactional
    public void submitReview(String email, ReviewRequestDto dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        AccommodationReservation res = reservationRepository.findById(dto.getReservationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!res.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        boolean paid = res.getPaymentStatus() == PaymentStatus.PAID
                || (res.getPaymentStatus() == null && res.getStatus() == AccommodationReservationStatus.CONFIRMED);
        if (!paid || res.getStatus() != AccommodationReservationStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reservation not eligible for review");
        }
        if (!res.getCheckOut().toLocalDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review only after checkout");
        }
        if (reviewRepository.findByUserIdAndReservationId(user.getId(), res.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Review already submitted");
        }
        String comment = dto.getComment() != null ? dto.getComment().trim() : "";
        if (comment.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment too long");
        }
        AccommodationReview rev = AccommodationReview.builder()
                .id(UUID.randomUUID())
                .accommodation(res.getAccommodation())
                .userId(user.getId())
                .reservationId(res.getId())
                .stars(dto.getStars())
                .comment(comment.isEmpty() ? null : comment)
                .build();
        reviewRepository.save(rev);
    }

    @Transactional(readOnly = true)
    public List<ClientReviewResponseDto> hostClientReviews(String email) {
        User host = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return reviewRepository.findAllForHost(host.getId()).stream()
                .map(rev -> {
                    User guest = userRepository.findById(rev.getUserId()).orElse(null);
                    String guestName = guest == null ? "Guest"
                            : ((guest.getFirstName() != null ? guest.getFirstName() : "") + " "
                            + (guest.getLastName() != null ? guest.getLastName() : "")).trim();
                    if (guestName.isBlank()) {
                        guestName = "Guest";
                    }
                    return ClientReviewResponseDto.builder()
                            .reviewId(rev.getId())
                            .stars(rev.getStars())
                            .comment(rev.getComment())
                            .createdAt(rev.getCreatedAt())
                            .accommodationTitle(rev.getAccommodation().getTitle())
                            .guestDisplayName(guestName)
                            .reservationId(rev.getReservationId())
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HostReservationDto> hostReservations(String email) {
        User host = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        List<AccommodationReservation> list = reservationRepository.findByHostIdForDashboard(host.getId());
        List<HostReservationDto> out = new ArrayList<>();
        for (AccommodationReservation r : list) {
            User guest = userRepository.findById(r.getUserId()).orElse(null);
            String guestName = guest == null ? "Guest"
                    : ((guest.getFirstName() != null ? guest.getFirstName() : "") + " "
                    + (guest.getLastName() != null ? guest.getLastName() : "")).trim();
            if (guestName.isBlank()) {
                guestName = guest != null && guest.getEmail() != null ? guest.getEmail() : "Guest";
            }
            Accommodation acc = r.getAccommodation();
            String roomSummary = r.getAssignedRoom() != null ? r.getAssignedRoom().getType().name() : "—";
            String payLabel = r.getPaymentStatus() != null ? r.getPaymentStatus().name() : "—";
            out.add(HostReservationDto.builder()
                    .reservationId(r.getId())
                    .bookingGroupId(r.getBookingGroupId())
                    .accommodationId(acc.getId())
                    .accommodationTitle(acc.getTitle())
                    .guestDisplayName(guestName)
                    .guestEmail(guest != null ? guest.getEmail() : null)
                    .checkIn(r.getCheckIn())
                    .checkOut(r.getCheckOut())
                    .guests(r.getGuests())
                    .status(r.getStatus().name())
                    .paymentStatus(payLabel)
                    .invoiceNumber(r.getInvoiceNumber())
                    .confirmationCode(r.getConfirmationCode())
                    .priceTotal(r.getPriceTotal())
                    .roomSummary(roomSummary)
                    .build());
        }
        return out;
    }

    private static String randomAlphanumeric(int len) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
