package fr.epita.yeea2.dto;

import lombok.Data;

@Data
public class JiraUpdateStatusRequest {
    private String jiraEmail;   // Email associated with the Jira account
    private String issueKey;    // Jira issue key (e.g., "PROJECT-123")
    private String newStatus;   // New status to transition to (e.g., "In Progress")
    private String cloudId;
    // Getters and Setters
}
