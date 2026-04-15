package tn.esprit.spring.baladna.accommodation.entity.enums;

public enum AccommodationType {
    GUEST_HOUSE,
    CAMPING,
    APARTMENT,
    FARM,
    /** Legacy or manual DB values; kept so Hibernate can read existing rows. */
    OTHER
}
