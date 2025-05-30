package fr.epita.yeea2.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@Document(collection = "users")
public class AppUser {
    @Id
    private String id;
    private String email;
    private String password; // hashed!
    private List<String> roles;
}
