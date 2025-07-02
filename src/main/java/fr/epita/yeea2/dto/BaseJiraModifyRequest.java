package fr.epita.yeea2.dto;

import lombok.Data;

@Data
public class BaseJiraModifyRequest {
    protected String jiraEmail;
    protected String summary;
    protected String description;
}
