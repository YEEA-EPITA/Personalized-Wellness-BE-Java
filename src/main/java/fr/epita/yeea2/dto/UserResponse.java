package fr.epita.yeea2.dto;

import fr.epita.yeea2.entity.AppUser;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserResponse {
    private String id;
    private String email;
    private List<String> roles;
    private String accessToken;

    public static UserResponse from(AppUser user, String token) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .roles(user.getRoles())
                .accessToken(token)
                .build();
    }
}
