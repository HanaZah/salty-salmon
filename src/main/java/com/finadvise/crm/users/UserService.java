package com.finadvise.crm.users;

import com.finadvise.crm.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final AdvisorRepository advisorRepository;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdvisorMapper advisorMapper;
    private final AdminMapper adminMapper;
    private final EmployeeIdGenerator employeeIdGenerator;

    /**
     * Creates a new advisor. Restricted to administrators.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AdvisorDTO createAdvisor(CreateAdvisorRequestDTO request) {
        if (advisorRepository.existsByIco(request.ico())) {
            throw new IllegalArgumentException("An advisor with this ICO already exists.");
        }
        if (advisorRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("A user with this email already exists.");
        }

        Long nextId = userRepository.getNextSequenceValue();
        String employeeId = employeeIdGenerator.encode(nextId);

        Advisor advisor = Advisor.builder()
                .id(nextId)
                .employeeId(employeeId)
                .passwordHash(passwordEncoder.encode(request.rawPassword()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .ico(request.ico())
                .phone(request.phone())
                .email(request.email())
                .isActive(true)
                .build();

        return advisorMapper.toDto(advisorRepository.save(advisor));
    }

    /**
     * Creates a new admin. Restricted to administrators.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AdminDTO createAdmin(CreateAdminRequestDTO request) {

        Long nextId = userRepository.getNextSequenceValue();
        String employeeId = employeeIdGenerator.encode(nextId);

        Admin admin = Admin.builder()
                .id(nextId)
                .employeeId(employeeId)
                .passwordHash(passwordEncoder.encode(request.rawPassword()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .isActive(true)
                .build();

        return adminMapper.toDto(adminRepository.save(admin));
    }

    /**
     * Updates an advisor's manager. Restricted to administrators.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void assignManager(Long advisorId, Long newManagerId) {
        Advisor advisor = advisorRepository.findById(advisorId)
                .orElseThrow(() -> new IllegalArgumentException("Advisor not found"));

        if (newManagerId == null) {
            advisor.setManager(null);
            advisorRepository.save(advisor);
            return;
        }

        Advisor newManager = advisorRepository.findById(newManagerId)
                .orElseThrow(() -> new IllegalArgumentException("Manager not found"));

        validateNoCircularReference(advisor, newManager);

        advisor.setManager(newManager);
        advisorRepository.save(advisor);
    }

    /**
     * Traverses the management hierarchy upwards to ensure the target advisor
     * is not an ancestor of the new manager.
     */
    private void validateNoCircularReference(Advisor advisor, Advisor newManager) {
        Objects.requireNonNull(advisor, "Advisor cannot be null");
        Objects.requireNonNull(newManager, "New manager cannot be null");

        if (Objects.equals(advisor.getId(), newManager.getId())) {
            throw new IllegalStateException("An advisor cannot manage themselves.");
        }

        Advisor currentAncestor = newManager.getManager();

        while (currentAncestor != null) {
            if (Objects.equals(currentAncestor.getId(), advisor.getId())) {
                throw new IllegalStateException(
                        String.format("Circular reference detected: %s %s is already managing someone in %s %s's chain.",
                                advisor.getFirstName(), advisor.getLastName(),
                                newManager.getFirstName(), newManager.getLastName())
                );
            }
            currentAncestor = currentAncestor.getManager();
        }
    }

    /**
     * Changes the password for the currently authenticated user.
     */
    @Transactional
    public void changePassword(String employeeId, ChangePasswordRequestDTO request) {
        User user = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 1. Verify the old password is correct
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("Incorrect current password.");
        }

        // 2. Prevent reusing the same password (optional but standard practice)
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("New password cannot be the same as the old password.");
        }

        // 3. Encode and save
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    /**
     * Updates the basic profile information of the currently authenticated user.
     */
    @Transactional
    public void updateProfile(String employeeId, UpdateProfileRequestDTO request) {
        User user = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.firstName() != null && !request.firstName().isBlank()) {
            user.setFirstName(request.firstName());
        }

        if (request.lastName() != null && !request.lastName().isBlank()) {
            user.setLastName(request.lastName());
        }

        if (user instanceof Advisor advisor && request.phone() != null && !request.phone().isBlank()) {
            advisor.setPhone(request.phone());
        }

        userRepository.save(user);
    }

    @Override
    @NonNull
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmployeeId(username)
                .orElseThrow(() -> new UsernameNotFoundException("User " + username + " not found."));

        return new CustomUserDetails(user);
    }

    public AdvisorDTO getAdvisorById(Long id) {
        return advisorMapper.toDto(
                advisorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Advisor not found")));
    }

}
