package fr.epita.yeea2.controller;

import fr.epita.yeea2.dto.*;
import fr.epita.yeea2.entity.PlatformCredential;
import fr.epita.yeea2.service.JiraService;
import fr.epita.yeea2.service.JwtService;
import fr.epita.yeea2.service.Utils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class JiraController {

    @Value("${jira.client-id}")
    private String clientId;

    @Value("${jira.client-secret}")
    private String clientSecret;

    @Value("${jira.redirect-uri}")
    private String redirectUri;

    @Value("${platform.redirectUrl}")
    private String successfulRedirectUrl;

    @Autowired
    private JiraService jiraService;

    private final JwtService jwtService;
    private final Utils utils;


    @GetMapping("/check-auth")
    public String checkAuth() {
        return "✅ Authenticated as: ";

    }

    @GetMapping("/login")
    public ResponseEntity<?> redirectToJira(
            @RequestHeader("Authorization") String authHeader,
//            @RequestParam("site") String siteUrl,  // selected Jira site
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
                "&scope=read:me%20read:jira-user%20read:jira-work%20write:jira-work%20delete:jira-work%20offline_access"+
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&prompt=consent" +
                "&state=" + encodedState;

        ApiResponse<String> response_ = new ApiResponse<>(200, "Jira redirected", authUrl);

        return ResponseEntity.ok(response_);
    }

    @GetMapping("/callback")
    public void handleJiraCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse response
    ) throws IOException {
        jiraService.exchangeCodeForTokens(code, state);
        response.sendRedirect(successfulRedirectUrl);
    }

    @GetMapping("/projects")
    public ResponseEntity<?> getJiraProjects(@RequestParam String jiraEmail,
                                             HttpServletRequest httpServletRequest) {
        try {
            String userId = utils.getUserIdFromHeader(httpServletRequest);
            List<JiraProjectResponse> projects = jiraService.getProjects(jiraEmail, userId);

            ApiResponse<List<JiraProjectResponse>> response = new ApiResponse<>(200, "Login successful", projects);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to fetch Jira projects",
                    "details", e.getMessage()
            ));
        }
    }

    @PostMapping("/tasks")
    public ResponseEntity<?> getTasksByProject(@RequestBody JiraIssueGetRequest request,
                                               HttpServletRequest httpServletRequest) {

        if (request.getJiraEmail() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "jiraEmail is required."));
        }
        String userId = utils.getUserIdFromHeader(httpServletRequest);
        try {
            List<JiraTaskResponse> tasks = jiraService.getTaskDetailsFromProject(request,userId);
            ApiResponse<List<JiraTaskResponse>> response = new ApiResponse<>(HttpStatus.OK.value(),"Retrieve tasks successfully", tasks);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<List<JiraTaskResponse>> errorResponse = new ApiResponse<>(400, "Failed to retrieve tasks: " + e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/task/create")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createTask(@RequestBody JiraCreateTaskRequest request,
                                                                       HttpServletRequest httpServletRequest) {
        try {
            String userId = utils.getUserIdFromHeader(httpServletRequest);
            Map<String, Object> createdTask = jiraService.createJiraTask(request, userId);
            ApiResponse<Map<String, Object>> response = new ApiResponse<>(200, "Task created successfully", createdTask);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<Map<String, Object>> error = new ApiResponse<>(400, "Failed to create Jira task", null);
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/task/update")
    public ResponseEntity<ApiResponse<String>> updateTask(@RequestBody JiraUpdateTaskRequest request,
                                                          HttpServletRequest httpServletRequest) {
        try {
            String userId = utils.getUserIdFromHeader(httpServletRequest);
            jiraService.updateJiraTask(
                    request, userId
            );
            ApiResponse<String> response = new ApiResponse<>(200, "Task updated successfully", null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<String> error = new ApiResponse<>(400, "Failed to update Jira task: " + e.getMessage(), null);
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/status/update")
    public ResponseEntity<Map<String, Object>> updateJiraStatus(HttpServletRequest httpServletRequest, @RequestBody JiraUpdateStatusRequest request) {
        // Retrieve the Jira credentials (platformCredential) from your database or authentication service
        String userId = utils.getUserIdFromHeader(httpServletRequest);
        PlatformCredential credential = jiraService.getJiraCredential(request.getJiraEmail(),userId);
        if (credential == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid Jira credentials"));
        }

        String cloudId = request.getCloudId();
        String accessToken = credential.getTokens().getAccessToken();

        try {
            // Update the status of the Jira issue
            jiraService.updateIssueStatus(request.getIssueKey(), request.getNewStatus(), accessToken, cloudId, userId);

            return ResponseEntity.ok(Map.of("message", "Jira issue status updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error updating Jira issue status", "error", e.getMessage()));
        }
    }

    @PostMapping("/task/delete")
    public ResponseEntity<ApiResponse<String>> deleteTask(@RequestBody JiraDeleteIssueRequest request,
                                                          HttpServletRequest httpServletRequest) {
        try {
            String userId = utils.getUserIdFromHeader(httpServletRequest);
            jiraService.deleteJiraTask(request.getJiraEmail(), request.getIssueKey(), userId);
            ApiResponse<String> response = new ApiResponse<>(200, "Task deleted successfully", null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<String> error = new ApiResponse<>(400, "Failed to delete Jira task: " + e.getMessage(), null);
            return ResponseEntity.badRequest().body(error);
        }
    }

}

