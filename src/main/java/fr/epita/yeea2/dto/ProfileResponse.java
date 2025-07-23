package fr.epita.yeea2.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder

public class ProfileResponse {
    private String email;
    private String firstName;
    private String lastName;
    private String provider;
    private String createdAt;
}
