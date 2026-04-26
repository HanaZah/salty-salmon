package com.finadvise.crm.users;

import com.finadvise.crm.common.ObfuscatedIdGenerator;
import com.finadvise.crm.common.OwnershipValidator;
import com.finadvise.crm.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    private final ObfuscatedIdGenerator obfuscatedIdGenerator;
    private final OwnershipValidator ownershipValidator;

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
        String employeeId = obfuscatedIdGenerator.encode(nextId);

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
        String employeeId = obfuscatedIdGenerator.encode(nextId);

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
                .orElseThrow(() -> new ResourceNotFoundException("Advisor not found"));

        if (newManagerId == null) {
            advisor.setManager(null);
            advisorRepository.save(advisor);
            return;
        }

        Advisor newManager = advisorRepository.findById(newManagerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

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
            throw new CircularManagementException("An advisor cannot manage themselves.");
        }

        Advisor currentAncestor = newManager.getManager();

        while (currentAncestor != null) {
            if (Objects.equals(currentAncestor.getId(), advisor.getId())) {
                throw new CircularManagementException(
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
                .orElseThrow(); // the employeeId comes from principal, so it's guaranteed to be present

        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("Incorrect current password.");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("New password cannot be the same as the old password.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    /**
     * Updates the basic profile information of the currently authenticated user.
     * Uses version-based optimistic locking to prevent race conditions.
     */
    @Transactional
    public void updateProfile(String employeeId, UpdateProfileRequestDTO request) {
        User user = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(); // the employeeId comes from principal, so it's guaranteed to be present

        if (!user.getVersion().equals(request.version())) {
            throw new ObjectOptimisticLockingFailureException(User.class, Objects.requireNonNull(user.getId()));
        }

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

    public AdvisorDTO getAdvisorByEmployeeId(String employeeId, String requesterId) {
        if(!ownershipValidator.canAccessUser(employeeId, requesterId)) {
            throw new ResourceNotFoundException("Advisor not found or access denied.");
        }

        return advisorMapper.toDto(
                advisorRepository.findByEmployeeId(employeeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Advisor not found or access denied.")));
    }

    public List<String> getActiveAdminEmails() {
        return adminRepository.findActiveEmails();
    }
}
