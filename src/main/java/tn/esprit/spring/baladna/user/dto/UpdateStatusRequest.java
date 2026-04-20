package tn.esprit.spring.baladna.user.dto;

import lombok.Data;
import tn.esprit.spring.baladna.user.entity.Status;

@Data
public class UpdateStatusRequest {
    private Status status;
}
