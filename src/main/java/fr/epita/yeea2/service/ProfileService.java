package fr.epita.yeea2.service;

import fr.epita.yeea2.dto.ProfileRequest;
import fr.epita.yeea2.dto.ProfileResponse;
import fr.epita.yeea2.entity.AppUser;
import fr.epita.yeea2.entity.PlatformCredential;
import fr.epita.yeea2.repository.PlatformCredentialRepository;
import fr.epita.yeea2.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final PlatformCredentialRepository platformCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileResponse getUserInfo(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        PlatformCredential platformCredential = platformCredentialRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Platform credential not found"));

        return ProfileResponse.builder()
                .email(Optional.ofNullable(user.getEmail()).orElse(""))
                .firstName(Optional.ofNullable(user.getFirstName()).orElse(""))
                .lastName(Optional.ofNullable(user.getLastName()).orElse(""))
                .provider(Optional.ofNullable(user.getProvider()).orElse(""))
                .createdAt(Optional.of(platformCredential.getCreatedAt().toString()).orElse(""))
                .build();
    }

    public void updateUserInfo(String email, ProfileRequest request) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("GOOGLE".equals(user.getProvider())) {
            throw new IllegalArgumentException("Google login users are not allowed to modify their information.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("The password does not match.");
        }

        String newEmail = request.getEmail();
        if (!user.getEmail().equals(newEmail)) {
            boolean emailExists = userRepository.findByEmail(newEmail).isPresent();
            if (emailExists) {
                throw new IllegalArgumentException("The new email is already in use.");
            }
        }

        user.updateFromRequest(request);
        userRepository.save(user);
    }

    // TODO: Soft delete
    public void deleteUser(String email, String password) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("GOOGLE".equals(user.getProvider())) {
            userRepository.delete(user);
            return;
        }

        if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("The password does not match.");
        }

        userRepository.delete(user);
    }

}
