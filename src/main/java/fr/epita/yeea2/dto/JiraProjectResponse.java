package fr.epita.yeea2.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraProjectResponse {
    String id;
    String name;
    String key;
    String cloudId;

    public JiraProjectResponse(String id, String name, String key) {
        this.id = id;
        this.name = name;
        this.key = key;
    }
}
