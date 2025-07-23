package fr.epita.yeea2.entity;

import fr.epita.yeea2.dto.ProfileRequest;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class AppUser {
    @Id
    private String id;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    private String firstName;
    private String lastName;

    @NotBlank(message = "Password is required")
    private String password; // hashed!
    private List<String> roles;
    private String provider;
    @CreatedDate
    private Instant createdAt;

    public void updateFromRequest(ProfileRequest req) {
        this.firstName = req.getFirstName();
        this.lastName = req.getLastName();
        this.email = req.getEmail();
    }
}
