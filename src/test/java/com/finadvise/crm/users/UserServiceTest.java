package com.finadvise.crm.users;

import com.finadvise.crm.common.ObfuscatedIdGenerator;
import com.finadvise.crm.common.ResourceConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private AdvisorRepository advisorRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AdvisorMapper advisorMapper;
    @Mock private ObfuscatedIdGenerator obfuscatedIdGenerator;
    @Mock private AdminRepository adminRepository;
    @Mock private AdminMapper adminMapper;

    @InjectMocks
    private UserService userService;

    @Captor private ArgumentCaptor<Advisor> advisorCaptor;
    @Captor private ArgumentCaptor<Admin> adminCaptor;
    @Captor private ArgumentCaptor<User> userCaptor;

    // --- CREATE ADVISOR & ADMIN ---

    @Test
    void createAdvisor_ThrowsException_WhenIcoAlreadyExists() {
        CreateAdvisorRequestDTO request = new CreateAdvisorRequestDTO(
                "John", "Doe", "12345678", "john@finadvise.com", "1234567890", "Pass123"
        );
        when(advisorRepository.existsByIco("12345678")).thenReturn(true);

        assertThatThrownBy(() -> userService.createAdvisor(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("ICO already exists");
    }

    @Test
    void createAdvisor_ThrowsException_WhenEmailAlreadyExists() {
        CreateAdvisorRequestDTO request = new CreateAdvisorRequestDTO(
                "John", "Doe", "12345678", "john@finadvise.com", "1234567890", "Pass123"
        );
        when(advisorRepository.existsByIco("12345678")).thenReturn(false);
        when(userRepository.existsByEmail("john@finadvise.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createAdvisor(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("email already exists");
    }

    @Test
    void createAdvisor_SavesAndReturnsDto_OnValidRequest() {
        CreateAdvisorRequestDTO request = new CreateAdvisorRequestDTO(
                "John", "Doe", "12345678", "john@finadvise.com", "1234567890", "Pass123"
        );
        Advisor savedAdvisor = Advisor.builder().employeeId("EMP-100").build();
        AdvisorDTO expectedDto = new AdvisorDTO(0, "EMP-100", "12345678", "John", "Doe", "1234567890", "john@finadvise.com", null, null);

        when(advisorRepository.existsByIco(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.getNextSequenceValue()).thenReturn(100L);
        when(obfuscatedIdGenerator.encode(100L)).thenReturn("EMP-100");
        when(passwordEncoder.encode("Pass123")).thenReturn("hashed-pass");
        when(advisorRepository.save(any(Advisor.class))).thenReturn(savedAdvisor);
        when(advisorMapper.toDto(savedAdvisor)).thenReturn(expectedDto);

        AdvisorDTO result = userService.createAdvisor(request);

        assertThat(result.employeeId()).isEqualTo("EMP-100");
    }

    @Test
    void createAdmin_ThrowsException_WhenEmailAlreadyExists() {
        CreateAdminRequestDTO request = new CreateAdminRequestDTO("Jane", "Doe", "jane@finadvise.com", "Pass123");
        when(userRepository.existsByEmail("jane@finadvise.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createAdmin(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("email already exists");
    }

    @Test
    void createAdmin_SavesAndReturnsDto_OnValidRequest() {
        CreateAdminRequestDTO request = new CreateAdminRequestDTO("Jane", "Doe", "jane@finadvise.com", "Pass123");
        Admin savedAdmin = Admin.builder().employeeId("ADM-100").build();
        AdminDTO expectedDto = new AdminDTO("ADM-100", "Jane@Doe.mail", "Jane", "Doe");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.getNextSequenceValue()).thenReturn(100L);
        when(obfuscatedIdGenerator.encode(100L)).thenReturn("ADM-100");
        when(passwordEncoder.encode("Pass123")).thenReturn("hashed-pass");
        when(adminRepository.save(any(Admin.class))).thenReturn(savedAdmin);
        when(adminMapper.toDto(savedAdmin)).thenReturn(expectedDto);

        AdminDTO result = userService.createAdmin(request);

        assertThat(result.employeeId()).isEqualTo("ADM-100");
        verify(adminRepository).save(adminCaptor.capture());
        assertThat(adminCaptor.getValue().getEmail()).isEqualTo("jane@finadvise.com");
    }

    // --- ASSIGN MANAGER ---

    @Test
    void assignManager_ThrowsException_WhenAssigningSelf() {
        Advisor self = Advisor.builder().employeeId("ADV_001").build();
        when(advisorRepository.findByEmployeeId("ADV_001")).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> userService.assignManager("ADV_001", "ADV_001"))
                .isInstanceOf(CircularManagementException.class)
                .hasMessageContaining("cannot manage themselves");
    }

    @Test
    void assignManager_ThrowsException_OnCircularReference() {
        Advisor topBoss = Advisor.builder().id(1L).employeeId("ADV_001").build();
        Advisor middleManager = Advisor.builder().id(2L).employeeId("ADV_002").manager(topBoss).build();
        Advisor bottomEmployee = Advisor.builder().id(3L).employeeId("ADV_003").manager(middleManager).build();

        when(advisorRepository.findByEmployeeId("ADV_001")).thenReturn(Optional.of(topBoss));
        when(advisorRepository.findByEmployeeId("ADV_003")).thenReturn(Optional.of(bottomEmployee));

        assertThatThrownBy(() -> userService.assignManager("ADV_001", "ADV_003"))
                .isInstanceOf(CircularManagementException.class)
                .hasMessageContaining("Circular reference detected");
    }

    @Test
    void assignManager_UpdatesAndSaves_OnValidHierarchy() {
        Advisor boss = Advisor.builder().id(1L).employeeId("ADV_001").build();
        Advisor employee = Advisor.builder().id(2L).employeeId("ADV_002").build();

        when(advisorRepository.findByEmployeeId("ADV_002")).thenReturn(Optional.of(employee));
        when(advisorRepository.findByEmployeeId("ADV_001")).thenReturn(Optional.of(boss));

        userService.assignManager("ADV_002", "ADV_001");

        verify(advisorRepository).save(advisorCaptor.capture());
        assertThat(advisorCaptor.getValue().getManager().getId()).isEqualTo(1L);
    }

    // --- DEACTIVATE, GET_ME, LIST ---

    @Test
    void deactivateUser_SetsIsActiveToFalse() {
        Advisor activeAdvisor = Advisor.builder().employeeId("EMP-001").isActive(true).build();
        when(userRepository.findByEmployeeId("EMP-001")).thenReturn(Optional.of(activeAdvisor));

        userService.deactivateUser("EMP-001");

        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().isActive()).isFalse();
    }

    @Test
    void getAllAdvisors_ReturnsPagedData() {
        Page<Advisor> page = new PageImpl<>(List.of(new Advisor()));
        when(advisorRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(advisorMapper.toDto(any())).thenReturn(new AdvisorDTO(
                0, "A", "1", "F", "L", "1", "e", null, null
        ));

        Page<AdvisorDTO> result = userService.getAllAdvisors(PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(advisorMapper, times(1)).toDto(any());
    }

    @Test
    void getMe_ReturnsAdminDto_WhenUserIsAdmin() {
        Admin admin = new Admin();
        when(userRepository.findByEmployeeId("ADM-001")).thenReturn(Optional.of(admin));
        when(adminMapper.toDto(admin)).thenReturn(new AdminDTO("ADM-001", "A@B.mail", "A", "B"));

        Object result = userService.getMe("ADM-001");

        assertThat(result).isInstanceOf(AdminDTO.class);
    }

    @Test
    void getMe_ReturnsAdvisorDto_WhenUserIsAdvisor() {
        Advisor advisor = new Advisor();
        when(userRepository.findByEmployeeId("ADV-001")).thenReturn(Optional.of(advisor));
        when(advisorMapper.toDto(advisor)).thenReturn(new AdvisorDTO(
                0, "ADV-001", "1", "F", "L", "1", "e", null, null
        ));

        Object result = userService.getMe("ADV-001");

        assertThat(result).isInstanceOf(AdvisorDTO.class);
    }

    // --- PROFILE & PASSWORD UPDATES ---

    @Test
    void changePassword_ThrowsInvalidPasswordException_WhenOldPasswordIsWrong() {
        ChangePasswordRequestDTO request = new ChangePasswordRequestDTO("WrongPass", "NewPass123");
        User mockUser = Admin.builder().employeeId("A1234567").passwordHash("hashed-old-pass").build();

        when(userRepository.findByEmployeeId("A1234567")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("WrongPass", "hashed-old-pass")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword("A1234567", request))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("Incorrect current password");
    }

    @Test
    void updateProfile_ThrowsOptimisticLockingException_WhenVersionsDoNotMatch() {
        UpdateProfileRequestDTO request = new UpdateProfileRequestDTO(1, "NewFirst", "NewLast", "9998887777");
        Advisor mockAdvisor = Advisor.builder().id(123L).employeeId("A1234567").version(0).build();

        when(userRepository.findByEmployeeId("A1234567")).thenReturn(Optional.of(mockAdvisor));

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> userService.updateProfile("A1234567", request));
    }

    @Test
    void loadUserByUsername_ReturnsUserDetails_WhenUserExists() {
        Admin admin = Admin.builder().employeeId("A1234567").passwordHash("hash").build();
        when(userRepository.findByEmployeeId("A1234567")).thenReturn(Optional.of(admin));

        UserDetails userDetails = userService.loadUserByUsername("A1234567");
        assertThat(userDetails.getUsername()).isEqualTo("A1234567");
    }
}