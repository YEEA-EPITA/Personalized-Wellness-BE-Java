package fr.epita.yeea2.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.epita.yeea2.constant.PlatformConstant;
import fr.epita.yeea2.constant.PlatformConstant.JiraConstant;
import fr.epita.yeea2.dto.*;
import fr.epita.yeea2.entity.PlatformCredential;
import fr.epita.yeea2.repository.PlatformCredentialRepository;
import fr.epita.yeea2.util.DateUtils;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
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

    private static final String DOC_TYPE = "doc";
    private static final int DOC_VERSION = 1;
    private static final String PARAGRAPH_TYPE = "paragraph";
    private static final String TEXT_TYPE = "text";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_VERSION = "version";
    private static final String FIELD_CONTENT = "content";

    private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.APPLICATION_JSON;
    private static final String URL_TEMPLATE_CREATE = "https://api.atlassian.com/ex/jira/%s/rest/api/3/issue";

    // URL templates
    private static final String ISSUE_UPDATE_URL_TEMPLATE = "https://api.atlassian.com/ex/jira/%s/rest/api/3/issue/%s";



    public List<JiraProjectResponse> getProjects(String jiraEmai) {
        PlatformCredential credential = platformCredentialRepository.findByPlatformEmailAndType(jiraEmai, PlatformConstant.JIRA).orElse(null);
        if (credential != null) {
            RestTemplate restTemplate = new RestTemplate();

            // Step 1: Get cloud ID
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(credential.getTokens().getAccessToken());
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            List<String> cloudIds = credential.getPlatformCloudIds();

            if (cloudIds.isEmpty() || cloudIds.size() ==0) {
                throw new RuntimeException("No accessible resources found.");
            }

            // Step 2: Fetch projects
            List<JiraProjectResponse> projects = new ArrayList<>();
            cloudIds.forEach(cloudId -> {
                ResponseEntity<Map> projectResponse = restTemplate.exchange(
                        "https://api.atlassian.com/ex/jira/" + cloudId + "/rest/api/3/project/search",
                        HttpMethod.GET,
                        entity,
                        Map.class
                );
                // Extract and simplify project info
                Object valuesObj = projectResponse.getBody().get("values");

                ObjectMapper objectMapper = new ObjectMapper();
                List<JiraProjectResponse> rawProjects = objectMapper.convertValue(
                        valuesObj,
                        new TypeReference<List<JiraProjectResponse>>() {}
                );
                rawProjects.forEach(p -> p.setCloudId(cloudId));

                projects.addAll(rawProjects);
            });

            return projects;
        }
        return Collections.emptyList();
//        }
//        else return Collections.emptyList();
    }

    public PlatformCredential exchangeCodeForTokens(String code, String encodedState) {
        // Decode system token from state
        String decodedState = new String(Base64.getUrlDecoder().decode(encodedState), StandardCharsets.UTF_8);
//        JSONObject stateJson = new JSONObject(decodedState);

        String systemToken = new String(Base64.getUrlDecoder().decode(encodedState), StandardCharsets.UTF_8);
//        String selectedSite = stateJson.getString("site"); // <-- important!


        String email = jwtService.extractUsername(systemToken);
        String userId = jwtService.extractUserId(systemToken);
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
        String jiraEmail = this.getJiraEmailFromAccessToken(accessToken);

        // Step 4: Fetch cloudId
//        HttpHeaders cloudHeaders = new HttpHeaders();
//        cloudHeaders.setBearerAuth(accessToken);
//        HttpEntity<Void> cloudEntity = new HttpEntity<>(cloudHeaders);
//
//        ResponseEntity<List> cloudResponse = restTemplate.exchange(
//                "https://api.atlassian.com/oauth/token/accessible-resources",
//                HttpMethod.GET,
//                cloudEntity,
//                List.class
//        );
//
//        if (cloudResponse.getBody() == null || cloudResponse.getBody().isEmpty()) {
//            throw new RuntimeException("No accessible resources found.");
//        }
        List<Map<String, Object>> accessibleResources= this.getAccessibleResources(accessToken);
// Then in accessible-resources matching:
        List<String> cloudIds = accessibleResources.stream()
                .map(resource -> resource.get("id").toString())
                .collect(Collectors.toList());
        // Step 5: Save everything to DB
        return this.saveOrUpdateJiraCredential(userId, email, accessToken, refreshToken, jiraEmail, atlassianUserId, cloudIds);
    }

    public List<Map<String, Object>> getAccessibleResources(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> cloudEntity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<List> cloudResponse = restTemplate.exchange(
                "https://api.atlassian.com/oauth/token/accessible-resources",
                HttpMethod.GET,
                cloudEntity,
                List.class
        );

        if (cloudResponse.getBody() == null || cloudResponse.getBody().isEmpty()) {
            throw new RuntimeException("No accessible resources found.");
        }

        // Cast the generic list to List<Map<String, Object>>
        List<Map<String, Object>> resources = (List<Map<String, Object>>) cloudResponse.getBody();
        return resources;
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

    public PlatformCredential saveOrUpdateJiraCredential(String userId,
                                                            String userEmail,
                                                         String accessToken,
                                                         String refreshToken,
                                                         String jiraEmail,
                                                         String atlassianUserId,
                                                         List<String> cloudIds) {
        PlatformCredential.Token token = PlatformCredential.Token.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
        PlatformCredential savedCredential = platformCredentialRepository.findByPlatformEmailAndType(jiraEmail, PlatformConstant.JIRA).map(existing -> {
            existing.setTokens(token);
            existing.setUpdatedAt(Instant.now());
            return platformCredentialRepository.save(existing);
        }).orElseGet(() -> {
            PlatformCredential newCredential = PlatformCredential.builder()
                    .type(PlatformConstant.JIRA)
                    .email(userEmail)
                    .connectorId(new ObjectId(userId))
                    .name(null) // set Jira name if available
                    .tokens(token)
                    .platformUserId(atlassianUserId)
                    .platformEmail(jiraEmail)
                    .platformCloudIds(cloudIds)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            return platformCredentialRepository.save(newCredential);
        });
        return savedCredential;
    }

    public List<JiraTaskResponse> getTaskDetailsFromProject(JiraIssueGetRequest request) {
        try {
            PlatformCredential credential = platformCredentialRepository.findByPlatformEmailAndType(request.getJiraEmail(), PlatformConstant.JIRA).orElse(null);
            if (credential != null) {
                RestTemplate restTemplate = new RestTemplate();

                // Step 1: Get Cloud ID
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(credential.getTokens().getAccessToken());
                headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                List<String> cloudIds = credential.getPlatformCloudIds();

                if (cloudIds.isEmpty() || cloudIds.size() ==0) {
                    throw new RuntimeException("No accessible resources found.");
                }

                String startDateStr;
                startDateStr = request.getStartDate() != null? DateUtils.formatDate(request.getStartDate()) : null;
                String endDateStr;
                endDateStr = request.getEndDate() != null ? DateUtils.formatDate(request.getEndDate()) : null;

                // Step 2: Get issues for the specified projects
                List<JiraTaskResponse> issues = new ArrayList<>();
                cloudIds.forEach(cloudId -> {
                    String url = this.buildSearchUrl(cloudId, request.getJiraProjects(),startDateStr,endDateStr);
                    try{
                    ResponseEntity<Map> response = restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            Map.class
                    );
                    if (response.getStatusCode().is2xxSuccessful()) {
                        List<Map<String, Object>> rawIssues = (List<Map<String, Object>>) response.getBody().get("issues");
                        List<JiraTaskResponse> simplifiedIssues = rawIssues.stream()
                                .map(i -> this.simplifyTask(i,cloudId)
                                )
                                .collect(Collectors.toList());
                        issues.addAll(simplifiedIssues);
                    }} catch (Exception ex){
                        //do nothing because fetching prj from another site
                    }
                });

                return issues;
            }
            return Collections.emptyList();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    private String buildSearchUrl(String cloudId, List<String> projectKeys, String startDateStr, String endDateStr) {
        String joinedKeys = projectKeys.stream()
                .map(key -> "\"" + key + "\"") // quote each key for safety
                .collect(Collectors.joining(", "));

        String jql;
        String safeKey = joinedKeys.replace("%", "\\%");
        if (startDateStr != null && endDateStr != null) {
             jql = String.format("project+IN+(%s)+AND+issuetype=%s+AND+((duedate>=%s+AND+duedate<=%s)+OR+(duedate+IS+EMPTY))",
                    safeKey,
                    fr.epita.yeea2.constant.PlatformConstant.JiraConstant.IssueType.Task,
                    startDateStr,
                    endDateStr,
                    fr.epita.yeea2.constant.PlatformConstant.JiraConstant.IssueStatus.Done);
        } else {
            jql = String.format("project+IN+(%s)+AND+issuetype=%s",
                    safeKey,
                    fr.epita.yeea2.constant.PlatformConstant.JiraConstant.IssueType.Task,
                    fr.epita.yeea2.constant.PlatformConstant.JiraConstant.IssueStatus.Done);
        }
//+AND+status!=%s
//        String encodedJql = URLEncoder.encode(jql, StandardCharsets.UTF_8);

        return String.format(
                "https://api.atlassian.com/ex/jira/%s/rest/api/3/search?jql=%s",
                cloudId,
                jql
        );
    }

    private JiraTaskResponse simplifyTask(Map<String, Object> issue, String cloudId) {
        Map<String, Object> fields = (Map<String, Object>) issue.get(JiraConstant.JiraField.FIELDS);

        String summary = (String) fields.get(fr.epita.yeea2.constant.PlatformConstant.JiraConstant.JiraField.SUMMARY);
        String dueDate = (String) fields.get(fr.epita.yeea2.constant.PlatformConstant.JiraConstant.JiraField.DUEDATE);
        String createdAt = (String) fields.get(fr.epita.yeea2.constant.PlatformConstant.JiraConstant.JiraField.CREATED);
        String updatedAt = (String) fields.get(fr.epita.yeea2.constant.PlatformConstant.JiraConstant.JiraField.UPDATED);
        String issueKey = (String) issue.get(fr.epita.yeea2.constant.PlatformConstant.JiraConstant.JiraField.KEY);
        Map<String, Object> creator = (Map<String, Object>) fields.get(JiraConstant.JiraField.CREATOR);
        String createdBy = creator != null ? (String) creator.get(JiraConstant.JiraField.DISPLAY_NAME) : "Unknown";

        Map<String, Object> project = (Map<String, Object>) fields.get(JiraConstant.JiraField.PROJECT);
        String projectKey = project != null ? (String) project.get(JiraConstant.JiraField.KEY) : null;

        Map<String, Object> issueTypeMap = (Map<String, Object>) fields.get(JiraConstant.JiraField.ISSUE_TYPE);
        String issueType = issueTypeMap != null ? (String) issueTypeMap.get(JiraConstant.JiraField.NAME) : null;

        Map<String, Object> statusMap = (Map<String, Object>) fields.get(JiraConstant.JiraField.STATUS);
        String status = statusMap != null ? (String) statusMap.get(JiraConstant.JiraField.NAME) : null;

        Map<String,Object> assigneeMap = (Map<String, Object>) fields.get(JiraConstant.JiraField.ASSIGNEE);
        String assignedBy = assigneeMap != null ? (String) assigneeMap.get(JiraConstant.JiraField.DISPLAY_NAME) : null;

        JiraProfileResponse assignedByResponse = null;
        if (assigneeMap != null) {
            assignedByResponse = new JiraProfileResponse();
            assignedByResponse.setName((String) assigneeMap.get(JiraConstant.JiraField.DISPLAY_NAME));
            assignedByResponse.setEmail((String) assigneeMap.get(JiraConstant.JiraField.EMAIL));
            assignedByResponse.setImgUrl(this.extractAvatar48x48(assigneeMap));
        }

        String description = null;
        try {
            Map<String, Object> descriptionMap = (Map<String, Object>) fields.get(JiraConstant.JiraField.DESCRIPTION);
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) descriptionMap.get("content");
            Map<String, Object> paragraph = contentList != null && !contentList.isEmpty() ? contentList.get(0) : null;
            List<Map<String, Object>> textList = paragraph != null ? (List<Map<String, Object>>) paragraph.get("content") : null;
            description = textList != null && !textList.isEmpty() ? (String) textList.get(0).get("text") : null;
        } catch (Exception e) {
            description = null;
        }
        return JiraTaskResponse.builder()
                .issueKey(issueKey)
                .summary(summary)
                .dueDate(dueDate)
                .createdBy(createdBy)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .projectKey(projectKey)
                .description(description)
                .status(status)
                .issueType(issueType)
                .cloudId(cloudId)
                .assignedBy(assignedByResponse)
                .build();    }
    public PlatformCredential getJiraCredential(String jiraEmail) {
        return platformCredentialRepository
                .findByPlatformEmailAndType(jiraEmail, PlatformConstant.JIRA)
                .orElse(null);
    }


    public String extractAvatar48x48(Map<String, Object> userData) {
        Map<String, Object> avatarUrls = (Map<String, Object>) userData.get(JiraConstant.JiraField.IMG);
        if (avatarUrls != null && avatarUrls.containsKey("48x48")) {
            return avatarUrls.get("48x48").toString();
        }
        return null;
    }

    public Map<String, Object> createJiraTask(JiraCreateTaskRequest request) {
        PlatformCredential credential = this.getJiraCredential(request.getJiraEmail());
        if (credential == null) return null;

        String cloudId = request.getCloudId();
        String url = String.format(URL_TEMPLATE_CREATE, cloudId);

        Map<String, Object> fields = Map.of(
                JiraConstant.JiraField.FIELDS, Map.of(
                        JiraConstant.JiraField.PROJECT, Map.of(JiraConstant.JiraField.KEY, request.getProjectKey()),
                        JiraConstant.JiraField.SUMMARY, request.getSummary(),
                        JiraConstant.JiraField.DESCRIPTION, buildDescriptionContent(request.getDescription()),
                        JiraConstant.JiraField.ISSUE_TYPE, Map.of(JiraConstant.JiraField.NAME, JiraConstant.IssueType.Task)
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(fields, this.buildHeaders(credential));
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        return response.getBody();
    }

    public Map<String, Object> updateJiraTask(JiraUpdateTaskRequest request) {
        PlatformCredential credential = this.getJiraCredential(request.getJiraEmail());
        if (credential == null) return null;

        String cloudId = request.getCloudId();
        String url = String.format(ISSUE_UPDATE_URL_TEMPLATE, cloudId, request.getIssueKey());

        Map<String, Object> fields = Map.of(
                fr.epita.yeea2.constant.PlatformConstant.JiraConstant.JiraField.FIELDS, Map.of(
                        fr.epita.yeea2.constant.PlatformConstant.JiraConstant.JiraField.SUMMARY, request.getSummary(),
                        fr.epita.yeea2.constant.PlatformConstant.JiraConstant.JiraField.DESCRIPTION, this.buildDescriptionContent(request.getDescription())
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(fields, this.buildHeaders(credential));
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Map.class);
        return response.getBody();
    }

    private Map<String, Object> buildDescriptionContent(String description) {
        return Map.of(
                FIELD_TYPE, DOC_TYPE,
                FIELD_VERSION, DOC_VERSION,
                FIELD_CONTENT, List.of(
                        Map.of(
                                FIELD_TYPE, PARAGRAPH_TYPE,
                                FIELD_CONTENT, List.of(
                                        Map.of(
                                                FIELD_TYPE, TEXT_TYPE,
                                                "text", description
                                        )
                                )
                        )
                )
        );
    }

    private HttpHeaders buildHeaders(PlatformCredential credential) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(credential.getTokens().getAccessToken());
        headers.setContentType(DEFAULT_MEDIA_TYPE);
        return headers;
    }


    public void deleteJiraTask(String jiraEmail, String issueKey) {
        PlatformCredential credential = getJiraCredential(jiraEmail);
        if (credential == null) return;

        String cloudId = credential.getPlatformCloudIds().get(0);
        String url = String.format("https://api.atlassian.com/ex/jira/%s/rest/api/3/issue/%s", cloudId, issueKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(credential.getTokens().getAccessToken());

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }

    public String getTransitionId(String issueKey, String targetStatus, String accessToken, String cloudId) {
        String url = "https://api.atlassian.com/ex/jira/" + cloudId + "/rest/api/3/issue/" + issueKey + "/transitions";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {}
        );

        List<Map<String, Object>> transitions = (List<Map<String, Object>>) response.getBody().get("transitions");
        for (Map<String, Object> transition : transitions) {
            if (transition.get("name").equals(targetStatus)) {
                return (String) transition.get("id");
            }
        }
        return null;
    }

    public void updateIssueStatus(String issueKey, String newStatus, String accessToken, String cloudId) {
        String transitionId = this.getTransitionId(issueKey, newStatus, accessToken, cloudId);
        if (transitionId == null) throw new RuntimeException("No transition found for status: " + newStatus);

        String url = "https://api.atlassian.com/ex/jira/" + cloudId + "/rest/api/3/issue/" + issueKey + "/transitions";

        Map<String, Object> transition = Map.of("id", transitionId);
        Map<String, Object> body = Map.of("transition", transition);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        restTemplate.postForEntity(url, entity, String.class);
    }


}
