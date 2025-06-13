package fr.epita.yeea2.controller;

import fr.epita.yeea2.dto.JiraIssueRequest;
import fr.epita.yeea2.entity.PlatformCredential;
import fr.epita.yeea2.service.JiraService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/jira")
public class JiraController {

    @Value("${jira.client-id}")
    private String clientId;

    @Value("${jira.client-secret}")
    private String clientSecret;

    @Value("${jira.redirect-uri}")
    private String redirectUri;
    @Autowired
    private JiraService jiraService;

    @PostMapping("/create")
    public String createJiraIssue(@RequestBody JiraIssueRequest request) {
        return jiraService.createIssue(request);
    }

    // Optional: Test endpoint with hardcoded values
//    @GetMapping("/create-sample")
//    public String createSampleJiraIssue() {
//        JiraIssueRequest.Fields.Project project = new JiraIssueRequest.Fields.Project("PROJKEY");
//        JiraIssueRequest.Fields.IssueType issueType = new JiraIssueRequest.Fields.IssueType("Task");
//
//        JiraIssueRequest.Fields fields = new JiraIssueRequest.Fields(
//                project,
//                "Test Summary",
//                "Test Description",
//                issueType
//        );
//
//        JiraIssueRequest request = new JiraIssueRequest(fields);
//        return jiraService.createIssue(request);
//    }

//    @GetMapping("/login")
//    public void redirectToJira(HttpServletResponse response) throws IOException, IOException {
//        String redirectUri = "http://localhost:8080/jira/callback";
//        String authUrl = "https://auth.atlassian.com/authorize" +
//                "?audience=api.atlassian.com" +
//                "&client_id=" + clientId +
//                "&scope=read%3Ajira-user%20read%3Ajira-work" +
//                "&redirect_uri=" + redirectUri +
//                "&response_type=code" +
//                "&prompt=consent";
//        response.sendRedirect(authUrl);
//    }

    @GetMapping("/login")
    public void redirectToJira(
            @RequestHeader("Authorization") String authHeader,
            HttpServletResponse response
    ) throws IOException {
        String systemToken = authHeader.replace("Bearer ", "");

        // Ideally: validate systemToken here
        // Optionally: extract userId and encode that instead

        // Encode the token to safely pass in URL
        String encodedState = java.util.Base64.getUrlEncoder().encodeToString(systemToken.getBytes());

        String authUrl = "https://auth.atlassian.com/authorize" +
                "?audience=api.atlassian.com" +
                "&client_id=" + clientId +
                "&scope=read%3Ajira-user%20read%3Ajira-work" +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&prompt=consent" +
                "&state=" + encodedState;

        response.sendRedirect(authUrl);
    }
//
//    @GetMapping("/callback")
//    public ResponseEntity<String> handleJiraCallback(@RequestParam String code) {
//        // 1. Prepare token exchange request
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        Map<String, String> requestBody = Map.of(
//                "grant_type", "authorization_code",
//                "client_id", clientId,
//                "client_secret", clientSecret,
//                "code", code,
//                "redirect_uri", redirectUri
//        );
//
//        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
//        RestTemplate restTemplate = new RestTemplate();
//
//        // 2. Exchange code for access token
//        ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
//                "https://auth.atlassian.com/oauth/token",
//                entity,
//                Map.class
//        );
//
//        // 3. Extract the token
//        String accessToken = (String) tokenResponse.getBody().get("access_token");
//        String refreshToken = (String) tokenResponse.getBody().get("refresh_token");
//
//        // 4. (Optional) Store it for future API use — e.g. in your DB linked to the user
//        // userService.storeJiraTokens(userId, accessToken, refreshToken);
//
//        return ResponseEntity.ok("Jira access token: " + accessToken);
//    }

//    @GetMapping("/callback")
//    public ResponseEntity<String> handleJiraCallback(
//            @RequestParam String code,
//            @RequestParam String state
//    ) {
//        // Decode the system token from state
//        String systemToken = new String(java.util.Base64.getUrlDecoder().decode(state));
//
//        // TODO: validate systemToken and extract userId if needed
//        // Example:
//        // String userId = jwtUtil.extractUserId(systemToken);
//
//        // Prepare request to exchange code for access token
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        Map<String, String> requestBody = Map.of(
//                "grant_type", "authorization_code",
//                "client_id", clientId,
//                "client_secret", clientSecret,
//                "code", code,
//                "redirect_uri", redirectUri
//        );
//
//        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
//        RestTemplate restTemplate = new RestTemplate();
//
//        ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
//                "https://auth.atlassian.com/oauth/token",
//                entity,
//                Map.class
//        );
//
//        String accessToken = (String) tokenResponse.getBody().get("access_token");
//        String refreshToken = (String) tokenResponse.getBody().get("refresh_token");
//
//        // (Optional) Save tokens for this system user
//        // userService.storeJiraTokens(userId, accessToken, refreshToken);
//
//        // Return both tokens
//        return ResponseEntity.ok("System token: " + systemToken + "\nJira access token: " + accessToken);
//    }

    @GetMapping("/callback")
    public ResponseEntity<PlatformCredential> handleJiraCallback(
            @RequestParam String code,
            @RequestParam String state
    ) {
        return ResponseEntity.ok(jiraService.exchangeCodeForTokens(code, state));
    }

    @GetMapping("/projects")
    public ResponseEntity<?> getJiraProjects(@RequestParam("token") String jiraAccessToken) {
        try {
            List<Map<String, Object>> projects = jiraService.getProjects(jiraAccessToken);
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to fetch Jira projects",
                    "details", e.getMessage()
            ));
        }
    }

}

