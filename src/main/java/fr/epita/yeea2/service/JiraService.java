package fr.epita.yeea2.service;

import fr.epita.yeea2.dto.JiraIssueRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class JiraService {

    @Value("${jira.base-url}")
    private String jiraBaseUrl;

    @Value("${jira.jwt-token}")
    private String jwtToken;

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
}
