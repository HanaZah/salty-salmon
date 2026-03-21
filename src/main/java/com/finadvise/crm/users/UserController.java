package com.finadvise.crm.users;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public AdvisorDTO getAdvisorById(@PathVariable Long id) {
        return userService.getAdvisorById(id);
    }

    @PostMapping("/new/admin")
    public AdminDTO createNewAdmin(@RequestBody CreateAdminRequestDTO request) {
        return userService.createAdmin(request);
    }

    @PostMapping("/new/advisor")
    public AdvisorDTO createNewAdvisor(@RequestBody CreateAdvisorRequestDTO request) {
        return userService.createAdvisor(request);
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequestDTO request, Principal principal) {
        userService.changePassword(principal.getName(), request);
        return ResponseEntity.noContent().build(); // Standard 204 No Content for successful updates
    }

    @PutMapping("/profile")
    public ResponseEntity<Void> updateProfile(@RequestBody UpdateProfileRequestDTO request, Principal principal) {
        userService.updateProfile(principal.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
