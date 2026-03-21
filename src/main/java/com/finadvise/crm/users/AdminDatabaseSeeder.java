package com.finadvise.crm.users;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDatabaseSeeder implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeIdGenerator employeeIdGenerator;

    @Value("${INITIAL_ADMIN_PASSWORD}")
    private String adminPassword;

    @Override
    public void run(String @NonNull ... args) {
        if (adminRepository.count() == 0) {
            log.info("No administrators found in the database. Bootstrapping default admin...");

            Long nextId = userRepository.getNextSequenceValue();
            String generatedEmployeeId = employeeIdGenerator.encode(nextId);

            Admin defaultAdmin = Admin.builder()
                    .id(nextId)
                    .employeeId(generatedEmployeeId)
                    .firstName("System")
                    .lastName("Administrator")
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .isActive(true)
                    .build();

            adminRepository.save(defaultAdmin);
            log.info("Default admin created successfully with Employee ID: {}", generatedEmployeeId);
        }
    }
}
