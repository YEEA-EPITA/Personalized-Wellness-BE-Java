package fr.epita.yeea2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ErrorResponse {
    private int statusCode;
    private String message;
    private ErrorDetails err;

    public static class ErrorDetails {
        private String type;

        // Constructor
        public ErrorDetails(String type) {
            this.type = type;
        }

        // Getter and Setter
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
