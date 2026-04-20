package tn.esprit.spring.baladna.event.entity.enums;

public enum EventCategory {

    MUSIC("Music concerts, gigs"),
    FOOD("Tastings, cooking classes"),
    OUTDOOR("Hiking, camping, nature events"),
    CULTURE("Heritage, traditions, local customs"),
    ART("Exhibitions, painting, galleries"),
    WORKSHOP("Educational / skill-building"),
    SPORT("Competitions, matches"),
    FESTIVAL("Large cultural or seasonal festivals"),
    TOUR("Guided tours (city, desert, historical)"),
    FAMILY("Family-friendly activities"),
    NIGHTLIFE("Parties, nightlife events"),
    THEATER("Plays, performances, cinema"),
    OTHER("Misc / uncategorized");

    private final String description;

    EventCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}