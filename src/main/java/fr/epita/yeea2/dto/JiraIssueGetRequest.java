package fr.epita.yeea2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
public class JiraIssueGetRequest {
    String jiraEmail;
    List<String> jiraProjects;
    Date startDate;
    Date endDate;
}
