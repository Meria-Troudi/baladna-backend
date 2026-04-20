package tn.esprit.spring.baladna.event.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventUserStateDTO {
    private Long eventId;
    private String state; // AVAILABLE | RESERVED | PAID | WAITLISTED | CANCELLED
    private boolean full;
    private boolean past;
    private boolean canBook;
    private boolean canPay;
    private boolean canCancel;
    private boolean canEdit;
}
