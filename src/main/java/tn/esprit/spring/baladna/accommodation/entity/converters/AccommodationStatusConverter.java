package tn.esprit.spring.baladna.accommodation.entity.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tn.esprit.spring.baladna.accommodation.entity.enums.AccommodationStatus;

import java.util.Locale;

@Converter(autoApply = false)
public class AccommodationStatusConverter implements AttributeConverter<AccommodationStatus, String> {

    @Override
    public String convertToDatabaseColumn(AccommodationStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public AccommodationStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return AccommodationStatus.DRAFT;
        }
        String raw = dbData.trim();
        try {
            return AccommodationStatus.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            String normalized = raw.toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
            try {
                return AccommodationStatus.valueOf(normalized);
            } catch (IllegalArgumentException ignored2) {
                return AccommodationStatus.INACTIVE;
            }
        }
    }
}
