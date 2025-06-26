package fr.epita.yeea2.controller;

import fr.epita.yeea2.dto.ApiResponse;
import fr.epita.yeea2.dto.AuthRequest;
import fr.epita.yeea2.dto.UserResponse;
import fr.epita.yeea2.entity.AppUser;
import fr.epita.yeea2.repository.UserRepository;
import fr.epita.yeea2.service.AppUserDetailsService;
import fr.epita.yeea2.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
@AllArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppUserDetailsService appUserDetailsService;


    @GetMapping("/")
    public String home() {
        return "Welcome!";
    }

    @GetMapping("/api/profile")
    public String getProfile(@AuthenticationPrincipal User user) {
        if (user == null) {
            return "No authenticated user";
        }
        return "Hello, " + user.getUsername() + "! You are authenticated with roles: " + user.getAuthorities();
    }

    @GetMapping("/google/profile")
    public String profile(@AuthenticationPrincipal OAuth2User user) {

        return "Hello " + user.getAttribute("name") + ", email: " + user.getAttribute("email");
    }


    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(@RequestBody AuthRequest request) {
        try {
            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            String jwt = jwtService.generateToken(authentication);

            AppUser user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            UserResponse userResponse = UserResponse.from(user, jwt);

            ApiResponse<UserResponse> response = new ApiResponse<>(200, "Login successful", userResponse);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException ex) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(401, "Invalid email or password", null));
        }
    }

    @PostMapping("/auth/refresh-token")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing refresh token");
        }

        String refreshToken = authHeader.substring(7);
        String username = jwtService.extractUsername(refreshToken);

        if (username != null) {
            UserDetails user = appUserDetailsService.loadUserByUsername(username);
            if (jwtService.isTokenValid(refreshToken, user)) {
                // 🔁 Wrap user in an Authentication object
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities());

                String newAccessToken = jwtService.generateToken(authentication);
                return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
            }
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid refresh token");
    }


    @PostMapping("/auth/signup")
    public ResponseEntity<ApiResponse<UserResponse>> signup(@RequestBody AuthRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse<>(400, "Email already exists", null));
        }

        AppUser newUser = new AppUser();
        newUser.setEmail(request.getEmail());
        newUser.setFirstName(request.getFirstName());
        newUser.setLastName(request.getLastName());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setRoles(List.of("ROLE_USER")); // default role

        userRepository.save(newUser);

        // Optional: generate token after signup
        Authentication authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        String jwt = jwtService.generateToken(authentication);

        UserResponse userResponse = UserResponse.from(newUser, jwt);
        ApiResponse<UserResponse> response = new ApiResponse<>(200, "Account created successfully", userResponse);

        return ResponseEntity.ok(response);
    }

}
