package fr.epita.yeea2.controller;


import fr.epita.yeea2.dto.*;
import fr.epita.yeea2.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> getCurrentUser() {
        String email = getEmailFromSecurityContext();
        ProfileResponse response = profileService.getUserInfo(email);
        return ResponseEntity.ok(new ApiResponse<>(200, "Successfully read user profile.", response));
    }

    @PutMapping("/me")
    public ResponseEntity<GeneralResponse> updateUser(@RequestBody ProfileRequest request) {
        String email = getEmailFromSecurityContext();

        try {
            profileService.updateUserInfo(email, request);
            return ResponseEntity.ok(new GeneralResponse(200, "Your profile has been successfully updated."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new GeneralResponse(400, e.getMessage()));
        }
    }

    @DeleteMapping("/me")
    public ResponseEntity<GeneralResponse> deleteUser(@RequestBody(required = false) ProfileDeleteRequest request) {
        String email = getEmailFromSecurityContext();

        try {
            String password = request != null ? request.getPassword() : null;
            profileService.deleteUser(email, password);
            return ResponseEntity.ok(new GeneralResponse(200, "Your profile has been successfully deleted."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new GeneralResponse(400, e.getMessage()));
        }
    }

    private String getEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
