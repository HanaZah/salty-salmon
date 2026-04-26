package com.finadvise.crm.users;

import org.springframework.stereotype.Component;

@Component
public class AdminMapper {
    public AdminDTO toDto(Admin admin) {
        return new AdminDTO(
                admin.getEmployeeId(),
                admin.getFirstName(),
                admin.getLastName()
        );
    }
}
