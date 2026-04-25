package tn.esprit.spring.baladna.accommodation.entity.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tn.esprit.spring.baladna.accommodation.entity.enums.RoomType;

import java.util.Locale;

@Converter(autoApply = false)
public class RoomTypeConverter implements AttributeConverter<RoomType, String> {

    @Override
    public String convertToDatabaseColumn(RoomType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public RoomType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return RoomType.STANDARD;
        }
        String raw = dbData.trim();
        try {
            return RoomType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            String normalized = raw.toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
            try {
                return RoomType.valueOf(normalized);
            } catch (IllegalArgumentException ignored2) {
                return RoomType.OTHER;
            }
        }
    }
}
