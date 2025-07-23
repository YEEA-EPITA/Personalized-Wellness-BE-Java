package fr.epita.yeea2.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileRequest {
    private String email;
    private String firstName;
    private String lastName;
    private String password;
}
