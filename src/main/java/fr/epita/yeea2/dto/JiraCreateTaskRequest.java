package fr.epita.yeea2.dto;

import lombok.Data;

@Data
public class JiraCreateTaskRequest extends BaseJiraModifyRequest {
    private String projectKey;
}

