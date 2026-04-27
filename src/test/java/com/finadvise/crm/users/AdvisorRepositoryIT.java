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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AdvisorRepositoryIT {

    @Container
    @ServiceConnection
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:slim-faststart");

    @Autowired
    private AdvisorRepository advisorRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void existsByIco_ReturnsTrue_WhenAdvisorWithIcoExists() {
        // Arrange
        Advisor advisor = Advisor.builder()
                .id(1L)
                .employeeId("A1234567")
                .passwordHash("hash")
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .email("john.doe@finadvise.com")
                .ico("12345678")
                .isActive(true)
                .build();

        entityManager.persistAndFlush(advisor);

        // Act
        boolean exists = advisorRepository.existsByIco("12345678");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void existsByIco_ReturnsFalse_WhenIcoDoesNotExist() {
        // Act
        boolean exists = advisorRepository.existsByIco("99999999");

        // Assert
        assertThat(exists).isFalse();
    }
}
