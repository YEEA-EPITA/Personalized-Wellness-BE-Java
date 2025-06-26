package fr.epita.yeea2.dto;

import lombok.Data;

@Data
public class UpdateJiraTaskRequest extends BaseJiraModifyRequest {
    private String issueKey;
}
