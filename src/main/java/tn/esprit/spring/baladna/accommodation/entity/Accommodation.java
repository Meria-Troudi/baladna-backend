package tn.esprit.spring.baladna.accommodation.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import tn.esprit.spring.baladna.accommodation.entity.converters.AccommodationStatusConverter;
import tn.esprit.spring.baladna.accommodation.entity.converters.AccommodationTypeConverter;
import tn.esprit.spring.baladna.accommodation.entity.enums.AccommodationStatus;
import tn.esprit.spring.baladna.accommodation.entity.enums.AccommodationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "accommodations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Accommodation {

    @Id
    @Column(name = "accommodation_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String address;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Column(name = "max_guests")
    private Integer maxGuests;

    @Column(columnDefinition = "TEXT")
    private String amenities;

    @Column(columnDefinition = "TEXT")
    private String rules;

    /** VARCHAR + converter: unknown DB values map to OTHER / INACTIVE instead of failing the whole query. */
    @Convert(converter = AccommodationTypeConverter.class)
    @Column(name = "type", nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
    @Builder.Default
    private AccommodationType type = AccommodationType.GUEST_HOUSE;

    @Convert(converter = AccommodationStatusConverter.class)
    @Column(name = "status", nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
    @Builder.Default
    private AccommodationStatus status = AccommodationStatus.ACTIVE;

    @Column(name = "host_id", nullable = false)
    private Long hostId;

    @Column(name = "cover_image_file_name", length = 255)
    private String coverImageFileName;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "accommodation", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<Room> rooms = new ArrayList<>();
}
