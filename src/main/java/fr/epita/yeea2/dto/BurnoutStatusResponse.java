package fr.epita.yeea2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class BurnoutStatusResponse {
    private String userId;
    private String userEmail;
    private LocalDate date;
    private int burnoutScore;
    private String riskLevel;

    private String recommendationMessage;
}
