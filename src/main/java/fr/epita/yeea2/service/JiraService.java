package fr.epita.yeea2.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import fr.epita.yeea2.dto.JiraIssueRequest;
import fr.epita.yeea2.dto.JiraTaskResponse;
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
import java.util.stream.Collectors;


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

    public List<Map<String, Object>> getProjects(String jiraEmai) {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//
//        if (authentication != null && authentication.getPrincipal().get) {
//            String email = jwt.getClaim("email"); // or "https://id.atlassian.com/systemAccountEmail"
//            System.out.println("✅ Email from JWT: " + email);

            PlatformCredential credential = platformCredentialRepository.findByPlatformEmailAndType(jiraEmai, "JIRA").orElse(null);
            if (credential != null) {
                RestTemplate restTemplate = new RestTemplate();

                // Step 1: Get cloud ID
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(credential.getPlatformToken().getAccessToken());
                headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                String cloudId = credential.getPlatformCloudId();

                if (cloudId == null || cloudId.isEmpty()) {
                    throw new RuntimeException("No accessible resources found.");
                }

                // Step 2: Fetch projects
                ResponseEntity<Map> projectResponse = restTemplate.exchange(
                        "https://api.atlassian.com/ex/jira/" + cloudId + "/rest/api/3/project/search",
                        HttpMethod.GET,
                        entity,
                        Map.class
                );
                // Extract and simplify project info
                List<Map<String, Object>> rawProjects = (List<Map<String, Object>>) projectResponse.getBody().get("values");

                List<Map<String, Object>> simplifiedProjects = rawProjects.stream()
                        .map(project -> Map.of(
                                "id", project.get("id"),
                                "key", project.get("key"),
                                "name", project.get("name")
                        ))
                        .collect(Collectors.toList());

                return simplifiedProjects;
            }
            return Collections.emptyList();
//        }
//        else return Collections.emptyList();
    }

    public PlatformCredential exchangeCodeForTokens(String code, String encodedState) {
        // Decode system token from state
        String systemToken = new String(Base64.getUrlDecoder().decode(encodedState), StandardCharsets.UTF_8);
        String email = jwtService.extractUsername(systemToken);

        // Step 1: Exchange authorization code for tokens
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

        // Step 2: Decode JWT
        DecodedJWT decoded = JWT.decode(accessToken);
        String atlassianUserId = decoded.getSubject();

        // Step 3: Fetch Jira email
        String jiraEmail = getJiraEmailFromAccessToken(accessToken);

        // Step 4: Fetch cloudId
        HttpHeaders cloudHeaders = new HttpHeaders();
        cloudHeaders.setBearerAuth(accessToken);
        HttpEntity<Void> cloudEntity = new HttpEntity<>(cloudHeaders);

        ResponseEntity<List> cloudResponse = restTemplate.exchange(
                "https://api.atlassian.com/oauth/token/accessible-resources",
                HttpMethod.GET,
                cloudEntity,
                List.class
        );

        if (cloudResponse.getBody() == null || cloudResponse.getBody().isEmpty()) {
            throw new RuntimeException("No accessible resources found.");
        }

        String cloudId = (String) ((Map<?, ?>) cloudResponse.getBody().get(0)).get("id");

        // Step 5: Save everything to DB
        return saveOrUpdateJiraCredential(email, accessToken, refreshToken, jiraEmail, atlassianUserId, cloudId);
    }


    public String getJiraEmailFromAccessToken(String accessToken) {
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(accessToken);
        authHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Void> authEntity = new HttpEntity<>(authHeaders);

        ResponseEntity<Map> userInfoResponse = restTemplate.exchange(
                "https://api.atlassian.com/me",
                HttpMethod.GET,
                authEntity,
                Map.class
        );

        if (!userInfoResponse.getStatusCode().is2xxSuccessful() || userInfoResponse.getBody() == null) {
            throw new RuntimeException("Failed to fetch user info.");
        }

        Map<String, Object> userInfo = userInfoResponse.getBody();

        // Step 3: Extract the email
        String jiraEmail = (String) userInfo.get("email"); // key might be "email" or "emailAddress"

        if (jiraEmail == null) {
            throw new RuntimeException("Email not found in user info.");
        }

        return jiraEmail;
    }

    public PlatformCredential saveOrUpdateJiraCredential(String userEmail,
                                                         String accessToken,
                                                         String refreshToken,
                                                         String jiraEmail,
                                                         String atlassianUserId,
                                                         String cloudId) {
        PlatformCredential.Token token = PlatformCredential.Token.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
        PlatformCredential savedCredential = platformCredentialRepository.findByPlatformEmailAndType(jiraEmail, "JIRA").map(existing -> {
            existing.setPlatformToken(token);
            existing.setUpdatedAt(Instant.now());
            return platformCredentialRepository.save(existing);
        }).orElseGet(() -> {
            PlatformCredential newCredential = PlatformCredential.builder()
                    .type("JIRA")
                    .userEmail(userEmail)
                    .name(null) // set Jira name if available
                    .platformToken(token)
                    .platformUserId(atlassianUserId)
                    .platformEmail(jiraEmail)
                    .platformCloudId(cloudId)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            return platformCredentialRepository.save(newCredential);
        });
        return savedCredential;
    }

    public List<JiraTaskResponse> getTaskDetailsFromProject(String jiraEmail, String projectKey) {
        PlatformCredential credential = platformCredentialRepository.findByPlatformEmailAndType(jiraEmail, "JIRA").orElse(null);
        if (credential != null) {
            RestTemplate restTemplate = new RestTemplate();

            // Step 1: Get Cloud ID
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(credential.getPlatformToken().getAccessToken());
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String cloudId = credential.getPlatformCloudId();

            if (cloudId == null || cloudId.isEmpty()) {
                throw new RuntimeException("No accessible resources found.");
            }

            // Step 2: Get issues for the specified project
            String url = String.format(
                    "https://api.atlassian.com/ex/jira/%s/rest/api/3/search?jql=project=%s+AND+issuetype=Task",
                    cloudId,
                    projectKey
            );

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            List<Map<String, Object>> rawIssues = (List<Map<String, Object>>) response.getBody().get("issues");

            return rawIssues.stream()
                    .map(this::simplifyTask)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private JiraTaskResponse simplifyTask(Map<String, Object> issue) {
        Map<String, Object> fields = (Map<String, Object>) issue.get("fields");

        String summary = (String) fields.get("summary");
        String dueDate = (String) fields.get("duedate");
        String createdAt = (String) fields.get("created");
        String updatedAt = (String) fields.get("updated");
        String issueKey = (String) issue.get("key");

        Map<String, Object> creator = (Map<String, Object>) fields.get("creator");
        String createdBy = creator != null ? (String) creator.get("displayName") : "Unknown";

        return new JiraTaskResponse(issueKey,summary, dueDate, createdBy, createdAt, updatedAt);
    }
    private PlatformCredential getJiraCredential(String jiraEmail) {
        return platformCredentialRepository
                .findByPlatformEmailAndType(jiraEmail, "JIRA")
                .orElse(null);
    }

    public Map<String, Object> createJiraTask(String jiraEmail,
                                              String projectKey,
                                              String summary,
                                              String description) {
        PlatformCredential credential = this.getJiraCredential(jiraEmail);
        if (credential == null) return null;

        String cloudId = credential.getPlatformCloudId();
        String url = String.format("https://api.atlassian.com/ex/jira/%s/rest/api/3/issue", cloudId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(credential.getPlatformToken().getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> fields = Map.of(
                "fields", Map.of(
                        "project", Map.of("key", projectKey),
                        "summary", summary,
                        "description", description,
                        "issuetype", Map.of("name", "Task")
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(fields, headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        return response.getBody();
    }
    public void updateJiraTask(String jiraEmail, String issueKey, String newSummary, String newDescription) {
        PlatformCredential credential = getJiraCredential(jiraEmail);
        if (credential == null) return;

        String cloudId = credential.getPlatformCloudId();
        String url = String.format("https://api.atlassian.com/ex/jira/%s/rest/api/3/issue/%s", cloudId, issueKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(credential.getPlatformToken().getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> fields = Map.of(
                "fields", Map.of(
                        "summary", newSummary,
                        "description", newDescription
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(fields, headers);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
    }

    public void deleteJiraTask(String jiraEmail, String issueKey) {
        PlatformCredential credential = getJiraCredential(jiraEmail);
        if (credential == null) return;

        String cloudId = credential.getPlatformCloudId();
        String url = String.format("https://api.atlassian.com/ex/jira/%s/rest/api/3/issue/%s", cloudId, issueKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(credential.getPlatformToken().getAccessToken());

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }

}
