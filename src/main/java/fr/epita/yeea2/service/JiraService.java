package fr.epita.yeea2.service;

import fr.epita.yeea2.dto.JiraIssueRequest;
import fr.epita.yeea2.entity.PlatformCredential;
import fr.epita.yeea2.repository.PlatformCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JiraService {

    @Value("${jira.base-url}")
    private String jiraBaseUrl;

    @Value("${jira.jwt-token}")
    private String jwtToken;

    @Value("${jira.client-id}")
    private String clientId;

    @Value("${jira.client-secret}")
    private String clientSecret;

    @Value("${jira.redirect-uri}")
    private String redirectUri;

    private final PlatformCredentialRepository platformCredentialRepository;

    private final JwtService jwtService;

    private final RestTemplate restTemplate = new RestTemplate();

    public String createIssue(JiraIssueRequest request) {
        //TODO
        // temporary authenticate with email
        String email = "gianglibra1710@gmail.com";
        String auth = email + ":" + jwtToken;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.setBearerAuth(jwtToken); // Or set header manually: "Authorization", "Bearer " + token
        headers.set("Authorization", "Basic " + encodedAuth);
        HttpEntity<JiraIssueRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                jiraBaseUrl + "/rest/api/3/issue",
                HttpMethod.POST,
                entity,
                String.class
        );

        return response.getBody();
    }

    public List<Map<String, Object>> getProjects(String jiraAccessToken) {
        RestTemplate restTemplate = new RestTemplate();

        // Step 1: Get cloud ID
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jiraAccessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List> cloudResponse = restTemplate.exchange(
                "https://api.atlassian.com/oauth/token/accessible-resources",
                HttpMethod.GET,
                entity,
                List.class
        );

        if (cloudResponse.getBody() == null || cloudResponse.getBody().isEmpty()) {
            throw new RuntimeException("No accessible resources found.");
        }

        String cloudId = (String) ((Map<?, ?>) cloudResponse.getBody().get(0)).get("id");

        // Step 2: Fetch projects
        ResponseEntity<Map> projectResponse = restTemplate.exchange(
                "https://api.atlassian.com/ex/jira/" + cloudId + "/rest/api/3/project/search",
                HttpMethod.GET,
                entity,
                Map.class
        );

        return (List<Map<String, Object>>) projectResponse.getBody().get("values");
    }

    public PlatformCredential exchangeCodeForTokens(String code, String encodedState) {
        // Decode system token from state
        String systemToken = new String(Base64.getUrlDecoder().decode(encodedState), StandardCharsets.UTF_8);
        String email = jwtService.extractUsername(systemToken);

        // Exchange authorization code for tokens
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = Map.of(
                "grant_type", "authorization_code",
                "client_id", clientId,
                "client_secret", clientSecret,
                "code", code,
                "redirect_uri", redirectUri
        );

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
                "https://auth.atlassian.com/oauth/token",
                entity,
                Map.class
        );

        String accessToken = (String) tokenResponse.getBody().get("access_token");
        String refreshToken = (String) tokenResponse.getBody().get("refresh_token");
        return saveOrUpdateJiraCredential(email, accessToken, refreshToken);
    }

    public PlatformCredential saveOrUpdateJiraCredential(String email, String accessToken, String refreshToken) {
        PlatformCredential.Token token = PlatformCredential.Token.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
        PlatformCredential savedCredential = platformCredentialRepository.findByEmailAndType(email, "JIRA").map(existing -> {
            existing.setTokens(token);
            existing.setUpdatedAt(Instant.now());
            return platformCredentialRepository.save(existing);
        }).orElseGet(() -> {
            PlatformCredential newCredential = PlatformCredential.builder()
                    .type("JIRA")
                    .email(email)
                    .name(null) // set Jira name if available
                    .tokens(token)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            return platformCredentialRepository.save(newCredential);
        });
        return savedCredential;
    }
}
