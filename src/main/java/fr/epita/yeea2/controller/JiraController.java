package fr.epita.yeea2.controller;

import fr.epita.yeea2.dto.JiraIssueRequest;
import fr.epita.yeea2.service.JiraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
}

