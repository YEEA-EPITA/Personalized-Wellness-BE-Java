package fr.epita.yeea2.controller;

import fr.epita.yeea2.dto.*;
import fr.epita.yeea2.entity.PlatformCredential;
import fr.epita.yeea2.service.JiraService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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

    @GetMapping("/check-auth")
    public String checkAuth() {
            return "✅ Authenticated as: ";

    }

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
                "&scope=read:me%20read:jira-user%20read:jira-work%20delete:jira-work%20offline_access&"+
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&prompt=consent" +
                "&state=" + encodedState;

        response.sendRedirect(authUrl);
    }

    @GetMapping("/callback")
    public ResponseEntity<PlatformCredential> handleJiraCallback(
            @RequestParam String code,
            @RequestParam String state
    ) {
        return ResponseEntity.ok(jiraService.exchangeCodeForTokens(code, state));
    }

    @GetMapping("/projects")
    public ResponseEntity<?> getJiraProjects(@RequestParam String jiraEmail) {
        try {
            List<Map<String, Object>> projects = jiraService.getProjects(jiraEmail);
            ApiResponse<List<Map<String, Object>>> response = new ApiResponse<>(200, "Login successful", projects);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to fetch Jira projects",
                    "details", e.getMessage()
            ));
        }
    }

    @PostMapping("/tasks")
    public ResponseEntity<?> getTasksByProject(@RequestBody Map<String, String> body) {
        String jiraEmail = body.get("jiraEmail");
        String projectKey = body.get("projectKey");

        if (jiraEmail == null || projectKey == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "jiraEmail and projectKey are required."));
        }

        try {
            List<JiraTaskResponse> tasks = jiraService.getTaskDetailsFromProject(jiraEmail, projectKey);
            ApiResponse<List<JiraTaskResponse>> response = new ApiResponse<>(HttpStatus.OK.value(),"Retrieve tasks successfully", tasks);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<List<JiraTaskResponse>> errorResponse = new ApiResponse<>(400, "Failed to retrieve tasks: " + e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/task/create")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createTask(@RequestBody CreateJiraTaskRequest request) {
        try {
            Map<String, Object> createdTask = jiraService.createJiraTask(
                    request.getJiraEmail(),
                    request.getProjectKey(),
                    request.getSummary(),
                    request.getDescription()
            );
            ApiResponse<Map<String, Object>> response = new ApiResponse<>(200, "Task created successfully", createdTask);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<Map<String, Object>> error = new ApiResponse<>(400, "Failed to create Jira task", null);
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/task/update")
    public ResponseEntity<ApiResponse<String>> updateTask(@RequestBody UpdateJiraTaskRequest request) {
        try {
            jiraService.updateJiraTask(
                    request.getJiraEmail(),
                    request.getIssueKey(),
                    request.getSummary(),
                    request.getDescription()
            );
            ApiResponse<String> response = new ApiResponse<>(200, "Task updated successfully", null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<String> error = new ApiResponse<>(400, "Failed to update Jira task: " + e.getMessage(), null);
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/task/delete")
    public ResponseEntity<ApiResponse<String>> deleteTask(@RequestBody JiraDeleteIssueRequest request) {
        try {
            jiraService.deleteJiraTask(request.getJiraEmail(), request.getIssueKey());
            ApiResponse<String> response = new ApiResponse<>(200, "Task deleted successfully", null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<String> error = new ApiResponse<>(400, "Failed to delete Jira task: " + e.getMessage(), null);
            return ResponseEntity.badRequest().body(error);
        }
    }

}

