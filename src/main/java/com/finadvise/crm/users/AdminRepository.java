package com.finadvise.crm.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    @Query(""" 
        SELECT u.firstName as firstName,
               u.lastName as lastName,
               u.email as email,
               u.phone as phone
        FROM User u
        WHERE TYPE(u) = Admin AND u.isActive = true
    """)
    List<AdminContact> findActiveAdminContacts();

    Boolean existsByEmployeeId(String employeeId);
}
