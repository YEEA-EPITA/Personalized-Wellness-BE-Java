package fr.epita.yeea2.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "platforms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformCredential {

    @Id
    private String id;
    private String type;      // e.g. GOOGLE, JIRA, GITHUB
    private String name;      // Full name (if applicable)
    private String userEmail;     // Optional (not all platforms use email)
    private String platformEmail;
    private String platformUserId;
    private Token platformToken;
    private String platformCloudId;

    private String connectorId; // Optional reference to integration connector

    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Token {
        private String accessToken;
        private String refreshToken;
        private String idToken; // optional (used by Google)
        private String tokenType;
        private String scope;
        private Long expiryDate;
        private Long refreshTokenExpiresIn;
    }
}
