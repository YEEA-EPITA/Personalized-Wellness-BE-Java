package fr.epita.yeea2.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class BurnoutStatusDailyResponse {
    private String userId;
    private String userEmail;
    private int burnoutScore;
    private String riskLevel;
    private String recommendationMessage;
    private int extendedWorkSessions;
    private int lackOfBreaks;
    private int nightWork;
    private int todayWorkload;
    private int frequentContextSwitching;
}
