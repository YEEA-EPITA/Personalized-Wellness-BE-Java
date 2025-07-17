package fr.epita.yeea2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JiraTaskResponse {
    private String issueKey;
    private String summary;
    private String dueDate;
    private String createdBy;
    private String createdAt;
    private String updatedAt;
    private String status;
    private String description;
    private String projectKey;
    private String issueType;
    private String cloudId;
    private JiraProfileResponse assignedBy;
}
