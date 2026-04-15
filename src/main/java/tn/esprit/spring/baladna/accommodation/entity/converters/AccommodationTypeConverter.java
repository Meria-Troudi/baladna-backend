package tn.esprit.spring.baladna.accommodation.entity.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tn.esprit.spring.baladna.accommodation.entity.enums.AccommodationType;

import java.util.Locale;

@Converter(autoApply = false)
public class AccommodationTypeConverter implements AttributeConverter<AccommodationType, String> {

    @Override
    public String convertToDatabaseColumn(AccommodationType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public AccommodationType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return AccommodationType.GUEST_HOUSE;
        }
        String raw = dbData.trim();
        try {
            return AccommodationType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            String normalized = raw.toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
            try {
                return AccommodationType.valueOf(normalized);
            } catch (IllegalArgumentException ignored2) {
                return AccommodationType.OTHER;
            }
        }
    }
}
