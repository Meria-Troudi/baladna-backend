package tn.esprit.spring.baladna.accommodation.entity.enums;

public enum RoomType {
    STANDARD,
    DOUBLE,
    SUITE,
    DORM,
    FAMILY,
    /** Legacy or manual DB values so Hibernate can load all rooms. */
    OTHER
}
