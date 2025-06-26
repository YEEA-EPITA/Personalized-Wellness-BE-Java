package fr.epita.yeea2.dto;

import lombok.Data;

@Data
public class CreateJiraTaskRequest extends BaseJiraModifyRequest {
    private String projectKey;
}

