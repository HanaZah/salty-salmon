package com.finadvise.crm.users;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryIT {

    @Container
    @ServiceConnection
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:slim-faststart");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByEmployeeId_ReturnsUser_WhenEmployeeIdExists() {
        // Arrange - We can use Admin here since it extends User
        Admin admin = Admin.builder()
                .id(100L)
                .employeeId("ADMIN-001")
                .passwordHash("hash")
                .firstName("Super")
                .lastName("User")
                .isActive(true)
                .build();

        entityManager.persistAndFlush(admin);

        // Act
        Optional<User> foundUser = userRepository.findByEmployeeId("ADMIN-001");

        // Assert
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getFirstName()).isEqualTo("Super");
    }

    @Test
    void findByEmployeeId_ReturnsEmpty_WhenEmployeeIdDoesNotExist() {
        // Act
        Optional<User> foundUser = userRepository.findByEmployeeId("GHOST-999");

        // Assert
        assertThat(foundUser).isEmpty();
    }

    @Test
    void getNextSequenceValue_ReturnsNextValue() {
        // Act
        Long firstValue = userRepository.getNextSequenceValue();
        Long secondValue = userRepository.getNextSequenceValue();

        // Assert
        assertThat(firstValue).isNotNull();
        assertThat(secondValue).isNotNull();
        assertThat(secondValue).isGreaterThan(firstValue);
    }
}