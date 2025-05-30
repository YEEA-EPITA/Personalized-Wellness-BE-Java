package fr.epita.yeea2.controller;

import com.nimbusds.jwt.SignedJWT;
import fr.epita.yeea2.dto.AuthRequest;
import fr.epita.yeea2.dto.AuthResponse;
import fr.epita.yeea2.entity.AppUser;
import fr.epita.yeea2.repository.UserRepository;
import fr.epita.yeea2.service.JwtService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@AllArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


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

    @GetMapping("/debug-alg")
    public ResponseEntity<String> debugJwtAlg(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String alg = getJwtAlgorithm(token);
            return ResponseEntity.ok("JWT alg = " + alg);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid token");
        }
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        Authentication authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String jwt = jwtService.generateToken(authentication);
        return ResponseEntity.ok(new AuthResponse(jwt));
    }

    private String getJwtAlgorithm(String token) throws Exception {
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.getHeader().getAlgorithm().getName();
    }

    @PostMapping("/auth/signup")
    public ResponseEntity<?> signup(@RequestBody AuthRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        AppUser newUser = new AppUser();
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setRoles(List.of("ROLE_USER")); // default role

        userRepository.save(newUser);
        return ResponseEntity.ok("Account created successfully");
    }

}
