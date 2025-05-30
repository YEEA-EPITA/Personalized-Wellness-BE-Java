package fr.epita.yeea2.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JiraIssueRequest {
    private Fields fields;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Fields {
        private Project project;
        private String summary;
        private JsonNode description; // ✅ use JsonNode for flexible JSON
        private IssueType issuetype;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Project {
            private String key;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class IssueType {
            private String name;
        }
    }
}
