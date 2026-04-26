package tn.esprit.spring.baladna.user.dto;

import lombok.Data;
import tn.esprit.spring.baladna.user.entity.Role;

@Data
public class UpdateRoleRequest {
    private Role role;
}
