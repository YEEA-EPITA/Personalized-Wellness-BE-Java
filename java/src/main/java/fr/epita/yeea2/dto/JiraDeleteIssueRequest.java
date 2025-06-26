package fr.epita.yeea2.dto;

import lombok.Data;

@Data
public class JiraDeleteIssueRequest {
    String jiraEmail;
    String issueKey;
}
