package com.finadvise.crm.users;

import com.finadvise.crm.common.ObfuscatedIdGenerator;
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
    private final ObfuscatedIdGenerator obfuscatedIdGenerator;

    @Value("${INITIAL_ADMIN_PASSWORD}")
    private String adminPassword;
    @Value("$INITIAL_ADMIN_PHONE")
    private String adminPhone;
    @Value("$INITIAL_ADMIN_EMAIL")
    private String adminEmail;

    @Override
    public void run(String @NonNull ... args) {
        if (adminRepository.count() == 0) {
            log.info("No administrators found in the database. Bootstrapping default admin...");

            Long nextId = userRepository.getNextSequenceValue();
            String generatedEmployeeId = obfuscatedIdGenerator.encode(nextId);

            Admin defaultAdmin = Admin.builder()
                    .id(nextId)
                    .employeeId(generatedEmployeeId)
                    .firstName("System")
                    .lastName("Administrator")
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .phone(adminPhone)
                    .email(adminEmail)
                    .isActive(true)
                    .build();

            adminRepository.save(defaultAdmin);
            log.info("Default admin created successfully with Employee ID: {}", generatedEmployeeId);
        }
    }
}
