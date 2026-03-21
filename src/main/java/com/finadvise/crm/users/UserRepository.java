package com.finadvise.crm.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmployeeId(String employeeId);

    @Query(value = "SELECT USER_SEQ.NEXTVAL FROM DUAL", nativeQuery = true)
    Long getNextSequenceValue();

    List<User> findAllByIsActive(boolean isActive);
}
