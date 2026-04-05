package com.finadvise.crm.users;

import com.finadvise.crm.common.ObfuscatedIdGenerator;
import com.finadvise.crm.common.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private AdvisorRepository advisorRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AdvisorMapper advisorMapper;
    @Mock
    private ObfuscatedIdGenerator obfuscatedIdGenerator;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<Advisor> advisorCaptor;
    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    void createAdvisor_ThrowsException_WhenIcoAlreadyExists() {

        CreateAdvisorRequestDTO request = new CreateAdvisorRequestDTO(
                "John", "Doe", "12345678", "john@finadvise.com", "1234567890", "Pass123"
        );
        when(advisorRepository.existsByIco("12345678")).thenReturn(true);

        assertThatThrownBy(() -> userService.createAdvisor(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ICO already exists");

        verify(advisorRepository, never()).existsByEmail(anyString());
        verify(advisorRepository, never()).save(any());
    }

    @Test
    void createAdvisor_SavesAndReturnsDto_OnValidRequest() {
        CreateAdvisorRequestDTO request = new CreateAdvisorRequestDTO(
                "John", "Doe", "12345678", "john@finadvise.com", "1234567890", "Pass123"
                );
        Advisor savedAdvisor = Advisor.builder().id(100L).build();
        AdvisorDTO expectedDto = new AdvisorDTO(
                100L,              // id
                0,                    // version
                "EMP-100",            // employeeId
                "12345678",           // ico
                "John",               // firstName
                "Doe",                // lastName
                "1234567890",         // phone
                "john@finadvise.com", // email
                null,                 // managerId (none on creation)
                null                  // statistics (none on creation)
        );

        when(advisorRepository.existsByIco(anyString())).thenReturn(false);
        when(advisorRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.getNextSequenceValue()).thenReturn(100L);
        when(obfuscatedIdGenerator.encode(100L)).thenReturn("EMP-100");
        when(passwordEncoder.encode("Pass123")).thenReturn("hashed-pass");
        when(advisorRepository.save(any(Advisor.class))).thenReturn(savedAdvisor);
        when(advisorMapper.toDto(savedAdvisor)).thenReturn(expectedDto);

        AdvisorDTO result = userService.createAdvisor(request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(100L);

        // Capture the exact entity passed to the repository to verify internal mapping
        verify(advisorRepository).save(advisorCaptor.capture());
        Advisor capturedAdvisor = advisorCaptor.getValue();

        assertThat(capturedAdvisor.getPasswordHash()).isEqualTo("hashed-pass");
        assertThat(capturedAdvisor.getEmployeeId()).isEqualTo("EMP-100");
    }

    @Test
    void assignManager_ThrowsException_WhenAssigningSelf() {
        Advisor self = Advisor.builder().id(1L).build();
        when(advisorRepository.findById(1L)).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> userService.assignManager(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot manage themselves");
    }

    @Test
    void assignManager_ThrowsException_OnCircularReference() {
        // Build the hierarchy: Top Boss -> Middle Manager -> Bottom Employee
        Advisor topBoss = Advisor.builder().id(1L).firstName("Top").lastName("Boss").build();
        Advisor middleManager = Advisor.builder().id(2L).firstName("Middle").lastName("Manager").manager(topBoss).build();
        Advisor bottomEmployee = Advisor.builder().id(3L).firstName("Bottom").lastName("Employee").manager(middleManager).build();

        // Attempting to make the Bottom Employee the manager of the Top Boss
        when(advisorRepository.findById(1L)).thenReturn(Optional.of(topBoss));
        when(advisorRepository.findById(3L)).thenReturn(Optional.of(bottomEmployee));

        assertThatThrownBy(() -> userService.assignManager(1L, 3L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Circular reference detected");

        verify(advisorRepository, never()).save(any());
    }

    @Test
    void loadUserByUsername_ReturnsUserDetails_WhenUserExists() {
        Admin admin = Admin.builder().employeeId("A1234567").passwordHash("hash").build();
        when(userRepository.findByEmployeeId("A1234567")).thenReturn(Optional.of(admin));

        UserDetails userDetails = userService.loadUserByUsername("A1234567");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("A1234567");
        assertThat(userDetails.getPassword()).isEqualTo("hash");
    }

    @Test
    void loadUserByUsername_ThrowsException_WhenUserDoesNotExist() {
        when(userRepository.findByEmployeeId("B1234567")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("B1234567"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("B1234567");
    }

    @Test
    void assignManager_UpdatesAndSaves_OnValidHierarchy() {
        Advisor boss = Advisor.builder().id(1L).build();
        Advisor employee = Advisor.builder().id(2L).build();

        when(advisorRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(advisorRepository.findById(1L)).thenReturn(Optional.of(boss));

        userService.assignManager(2L, 1L);

        verify(advisorRepository).save(advisorCaptor.capture());
        Advisor savedEmployee = advisorCaptor.getValue();

        assertThat(savedEmployee.getManager()).isNotNull();
        assertThat(savedEmployee.getManager().getId()).isEqualTo(1L);
    }

    @Test
    void getAdvisorById_ThrowsResourceNotFound_WhenIdDoesNotExist() {
        when(advisorRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getAdvisorById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Advisor not found");

        verify(advisorMapper, never()).toDto(any());
    }


    @Test
    void changePassword_UpdatesPassword_WhenOldPasswordIsCorrect() {
        ChangePasswordRequestDTO request = new ChangePasswordRequestDTO("OldPass123", "NewPass123");
        User mockUser = Admin.builder()
                .id(333L)
                .employeeId("A1234567")
                .passwordHash("hashed-old-pass")
                .build();

        when(userRepository.findByEmployeeId("A1234567")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("OldPass123", "hashed-old-pass")).thenReturn(true);
        when(passwordEncoder.matches("NewPass123", "hashed-old-pass")).thenReturn(false);
        when(passwordEncoder.encode("NewPass123")).thenReturn("hashed-new-pass");

        userService.changePassword("A1234567", request);

        verify(userRepository).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();

        assertEquals("hashed-new-pass", updatedUser.getPasswordHash());
        assertEquals(333L, updatedUser.getId());
    }

    @Test
    void changePassword_ThrowsInvalidPasswordException_WhenOldPasswordIsWrong() {
        ChangePasswordRequestDTO request = new ChangePasswordRequestDTO("WrongPass", "NewPass123");
        User mockUser = Admin.builder().employeeId("A1234567").passwordHash("hashed-old-pass").build();

        when(userRepository.findByEmployeeId("A1234567")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("WrongPass", "hashed-old-pass")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword("A1234567", request))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("Incorrect current password");

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_ThrowsInvalidPasswordException_WhenNewPasswordIsSameAsOld() {
        ChangePasswordRequestDTO request = new ChangePasswordRequestDTO("SamePass123", "SamePass123");
        User mockUser = Admin.builder().employeeId("A1234567").passwordHash("hashed-pass").build();

        when(userRepository.findByEmployeeId("A1234567")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("SamePass123", "hashed-pass")).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword("A1234567", request))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("New password cannot be the same as the old password");

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_UpdatesPhone_WhenUserIsAdvisor() {
        UpdateProfileRequestDTO request = new UpdateProfileRequestDTO(
                0, "NewFirst", "NewLast", "9998887777");
        Advisor mockAdvisor = Advisor.builder()
                .employeeId("A1234567")
                .firstName("Old")
                .lastName("Name")
                .phone("111")
                .email("old@name.mail")
                .version(0)
                .build();

        when(userRepository.findByEmployeeId("A1234567")).thenReturn(Optional.of(mockAdvisor));

        userService.updateProfile("A1234567", request);

        verify(userRepository).save(mockAdvisor);
        assertThat(mockAdvisor.getFirstName()).isEqualTo("NewFirst");
        assertThat(mockAdvisor.getPhone()).isEqualTo("9998887777");
    }

    @Test
    void updateProfile_IgnoresPhone_WhenUserIsAdmin() {
        UpdateProfileRequestDTO request = new UpdateProfileRequestDTO(
                0, "NewFirst", "NewLast", "9998887777");
        Admin mockAdmin = Admin.builder()
                .employeeId("B1234567")
                .firstName("Old")
                .lastName("Name")
                .phone("123456789")
                .email("old@name.mail")
                .version(0)
                .build();

        when(userRepository.findByEmployeeId("B1234567")).thenReturn(Optional.of(mockAdmin));

        userService.updateProfile("B1234567", request);

        verify(userRepository).save(mockAdmin);
        assertThat(mockAdmin.getFirstName()).isEqualTo("NewFirst");
    }

    @Test
    void updateProfile_ThrowsOptimisticLockingException_WhenVersionsDoNotMatch() {
        UpdateProfileRequestDTO request = new UpdateProfileRequestDTO(
                1, "NewFirst", "NewLast", "9998887777");
        Advisor mockAdvisor = Advisor.builder()
                .id(123L)
                .employeeId("A1234567")
                .version(0)
                .build();

        when(userRepository.findByEmployeeId("A1234567")).thenReturn(Optional.of(mockAdvisor));

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            userService.updateProfile("A1234567", request);
        });

        verify(userRepository, never()).save(any());
    }
}
