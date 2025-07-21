package fr.epita.yeea2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class BurnoutStatusResponse {
    private String userId;
    private String userEmail;
    private int burnoutScore;
    private String riskLevel;
    private String recommendationMessage;
}
