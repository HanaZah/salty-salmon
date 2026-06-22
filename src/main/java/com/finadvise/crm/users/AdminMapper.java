package com.finadvise.crm.users;

import org.springframework.stereotype.Component;

@Component
public class AdminMapper {
    public AdminDTO toDto(Admin admin) {
        return new AdminDTO(
                admin.getEmployeeId(),
                admin.getEmail(),
                admin.getFirstName(),
                admin.getLastName()
        );
    }

    public AdminContactDTO toContactDto(AdminContact adminContact) {
        return new AdminContactDTO(
                adminContact.getFirstName(),
                adminContact.getLastName(),
                adminContact.getPhone(),
                adminContact.getEmail()
        );
    }
}
