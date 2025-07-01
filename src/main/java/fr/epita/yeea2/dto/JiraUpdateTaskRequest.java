package fr.epita.yeea2.dto;

import lombok.Data;

@Data
public class JiraUpdateTaskRequest extends BaseJiraModifyRequest {
    private String issueKey;
}
