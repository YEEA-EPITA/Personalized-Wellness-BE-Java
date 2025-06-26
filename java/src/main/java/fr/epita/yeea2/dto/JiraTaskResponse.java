package fr.epita.yeea2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JiraTaskResponse {
    private String issueKey;
    private String summary;
    private String dueDate;
    private String createdBy;
    private String createdAt;
    private String updatedAt;

}
