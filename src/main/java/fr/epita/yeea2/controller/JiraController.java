package fr.epita.yeea2.controller;

import fr.epita.yeea2.dto.JiraIssueRequest;
import fr.epita.yeea2.service.JiraService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/jira")
public class JiraController {

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

    @GetMapping("/login")
    public void redirectToJira(HttpServletResponse response) throws IOException, IOException {
        String clientId = "<your-client-id>";
        String redirectUri = "http://localhost:8080/jira/callback";
        String authUrl = "https://auth.atlassian.com/authorize" +
                "?audience=api.atlassian.com" +
                "&client_id=" + clientId +
                "&scope=read%3Ajira-user%20read%3Ajira-work" +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&prompt=consent";
        response.sendRedirect(authUrl);
    }

    @PostMapping("/jira/callback")
    public ResponseEntity<String> handleJiraCallback(@RequestParam String code) {
        String clientId = "6cXUVZvKkz7VazrMitDqS2zrEtGyjkDg";
        String clientSecret = "ATOAfR2ymt2LREOwEyBpHirpA9g6G_IkiyqqtU7bmuThYohcOIZgpW6fnI2ZazulWu2v26CF31D1";
        String redirectUri = "http://localhost:8080/jira/callback";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of(
                "grant_type", "authorization_code",
                "client_id", clientId,
                "client_secret", clientSecret,
                "code", code,
                "redirect_uri", redirectUri
        );

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        return new RestTemplate().postForEntity(
                "https://auth.atlassian.com/oauth/token", entity, String.class
        );
    }


}

