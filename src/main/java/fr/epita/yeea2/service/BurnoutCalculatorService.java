package fr.epita.yeea2.service;

import fr.epita.yeea2.dto.BurnoutStatusDailyResponse;
import fr.epita.yeea2.dto.BurnoutStatusResponse;
import fr.epita.yeea2.entity.AppUser;
import fr.epita.yeea2.entity.BurnoutStatus;
import fr.epita.yeea2.entity.RecoveryRecommendationType;
import fr.epita.yeea2.entity.TaskStatus;
import fr.epita.yeea2.repository.BurnoutStatusRepository;
import fr.epita.yeea2.repository.TaskStatusRepository;
import fr.epita.yeea2.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BurnoutCalculatorService {
    private final UserRepository userRepository;
    private final TaskStatusRepository taskStatusRepository;
    private final BurnoutStatusRepository burnoutStatusRepository;

    public BurnoutStatusDailyResponse calculateDailyBurnoutScore() {
        String email = getEmailFromSecurityContext();
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String userId = user.getId();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        List<TaskStatus> todayTasks = taskStatusRepository.findByUserIdAndStartTimeBetween(userId, startOfDay, endOfDay);

        long extendedWorkSessions = todayTasks.stream()
                .filter(t -> t.getEndTime() != null)
                .filter(t -> Duration.between(t.getStartTime(), t.getEndTime()).toHours() >= 3)
                .count();

        long breakCount = todayTasks.stream().filter(TaskStatus::isBreak).count();

        long nightWorkCount = todayTasks.stream()
                .filter(t -> t.getStartTime() != null && t.getStartTime().getHour() >= 20)
                .count();

        long todayWorkedMinutes = todayTasks.stream()
                .filter(t -> t.getEndTime() != null && !t.isBreak())
                .mapToLong(t -> Duration.between(t.getStartTime(), t.getEndTime()).toMinutes())
                .sum();

        long frequentContextSwitching = todayTasks.stream()
                .filter(t -> "IN_PROGRESS".equals(t.getStatus()))
                .count();

        int extendedWorkSessionsPoint = extendedWorkSessions > 0 ? 20 : 0;
        int lackOfBreaksPoint = (breakCount == 0) ? 20 : (breakCount == 1 ? 10 : 0);
        int nightWorkPoint = (nightWorkCount == 0) ? 0 : (nightWorkCount <= 2 ? 10 : 20);
        int todayWorkloadPoint = (todayWorkedMinutes <= 480) ? 0 : (todayWorkedMinutes <= 600 ? 10 : 20);
        int contextSwitchPoint = (frequentContextSwitching > 5) ? 20 : 0;

        int score = extendedWorkSessionsPoint + lackOfBreaksPoint + nightWorkPoint + todayWorkloadPoint + contextSwitchPoint;
        String level = (score < 40) ? "Normal" : (score < 70 ? "Caution" : "High");
        RecoveryRecommendationType recType = mapRiskLevelToRecovery(level);

        return BurnoutStatusDailyResponse.builder()
                .userId(userId)
                .userEmail(user.getEmail())
                .burnoutScore(score)
                .riskLevel(level)
                .recommendationMessage(recType.getMessage())
                .extendedWorkSessions(extendedWorkSessionsPoint)
                .lackOfBreaks(lackOfBreaksPoint)
                .nightWork(nightWorkPoint)
                .todayWorkload(todayWorkloadPoint)
                .frequentContextSwitching(contextSwitchPoint)
                .build();
    }

    public BurnoutStatusResponse calculateWeeklyBurnoutScore() {
        String email = getEmailFromSecurityContext();
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String userId = user.getId();

        LocalDateTime startOfWeek = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime endOfWeek = LocalDate.now().with(DayOfWeek.SUNDAY).atTime(LocalTime.MAX);
        List<TaskStatus> weeklyTasks = taskStatusRepository.findByUserIdAndStartTimeBetween(userId, startOfWeek, endOfWeek);

        long totalWorkedMinutes = weeklyTasks.stream()
                .filter(t -> t.getEndTime() != null && !t.isBreak())
                .mapToLong(t -> Duration.between(t.getStartTime(), t.getEndTime()).toMinutes())
                .sum();

        long totalBreakMinutes = weeklyTasks.stream()
                .filter(TaskStatus::isBreak)
                .mapToLong(t -> Duration.between(t.getStartTime(), t.getEndTime()).toMinutes())
                .sum();

        long nightWorkCount = weeklyTasks.stream()
                .filter(t -> t.getStartTime() != null && t.getStartTime().getHour() >= 20)
                .count();

        long shortTasks = weeklyTasks.stream()
                .filter(t -> !t.isBreak())
                .filter(t -> t.getStartTime() != null && t.getEndTime() != null)
                .filter(t -> Duration.between(t.getStartTime(), t.getEndTime()).toMinutes() < 15)
                .count();

        int workloadPoint = (totalWorkedMinutes <= 2400) ? 0 : (totalWorkedMinutes <= 3000) ? 10 : 20;
        int nightWorkPoint = (nightWorkCount == 0) ? 0 : (nightWorkCount <= 3) ? 10 : 20;
        int contextSwitchPoint = shortTasks > 5 ? 20 : 0;
        int breakPoint = totalBreakMinutes < 120 ? 20 : 0;

        int score = workloadPoint + breakPoint + nightWorkPoint + contextSwitchPoint;
        score = Math.min(score, 100);

        String level = (score < 40) ? "Normal" : (score < 70 ? "Caution" : "High");
        RecoveryRecommendationType recType = mapRiskLevelToRecovery(level);

        if (weeklyTasks.isEmpty()) {
            return BurnoutStatusResponse.builder()
                    .userId(userId)
                    .userEmail(user.getEmail())
                    .burnoutScore(0)
                    .riskLevel("NoData")
                    .recommendationMessage("No activity data found this week.")
                    .build();
        }

        BurnoutStatus status = BurnoutStatus.builder()
                .userId(userId)
                .burnoutScore(score)
                .riskLevel(level)
                .recommendationType(recType)
                .calculatedDate(LocalDate.now())
                .build();

        burnoutStatusRepository.save(status);

        return BurnoutStatusResponse.builder()
                .userId(userId)
                .userEmail(user.getEmail())
                .burnoutScore(score)
                .riskLevel(level)
                .recommendationMessage(recType.getMessage())
                .build();
    }

    private RecoveryRecommendationType mapRiskLevelToRecovery(String level) {
        return switch (level) {
            case "High" -> RecoveryRecommendationType.SLEEP;
            case "Caution" -> RecoveryRecommendationType.STRETCH;
            default -> RecoveryRecommendationType.NORMAL;
        };
    }

    private String getEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    public void createMockTodayTasks() {
        String email = getEmailFromSecurityContext();
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String userId = user.getId();

        LocalDateTime now = LocalDateTime.now();
        List<TaskStatus> mockTasks = List.of(
                TaskStatus.builder()
                        .userId(userId)
                        .startTime(now.minusHours(5))
                        .endTime(now.minusHours(2))
                        .status("DONE")
                        .isBreak(false)
                        .build(),
                TaskStatus.builder()
                        .userId(userId)
                        .startTime(now.minusHours(1))
                        .endTime(now)
                        .status("IN_PROGRESS")
                        .isBreak(false)
                        .build(),
                TaskStatus.builder()
                        .userId(userId)
                        .startTime(now.minusMinutes(45))
                        .endTime(now.minusMinutes(15))
                        .status("DONE")
                        .isBreak(true)
                        .build(),
                TaskStatus.builder()
                        .userId(userId)
                        .startTime(now.withHour(21))
                        .endTime(now.withHour(22))
                        .status("DONE")
                        .isBreak(false)
                        .build()
        );

        taskStatusRepository.saveAll(mockTasks);
    }

    public void createMockWeeklyTasks() {
        String email = getEmailFromSecurityContext();
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String userId = user.getId();

        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);

        List<TaskStatus> weeklyTasks = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate workDay = monday.plusDays(i);
            LocalDateTime startTime = workDay.atTime(9, 0);
            LocalDateTime endTime = workDay.atTime(18, 0);

            // 하루 9시간 근무, 하루 1회 break
            weeklyTasks.add(TaskStatus.builder()
                    .userId(userId)
                    .startTime(startTime)
                    .endTime(endTime)
                    .status("DONE")
                    .isBreak(false)
                    .build());

            weeklyTasks.add(TaskStatus.builder()
                    .userId(userId)
                    .startTime(workDay.atTime(12, 0))
                    .endTime(workDay.atTime(12, 30))
                    .status("DONE")
                    .isBreak(true)
                    .build());

            // context switching
            weeklyTasks.add(TaskStatus.builder()
                    .userId(userId)
                    .startTime(workDay.atTime(15, 0))
                    .endTime(workDay.atTime(16, 0))
                    .status("IN_PROGRESS")
                    .isBreak(false)
                    .build());
        }

        taskStatusRepository.saveAll(weeklyTasks);
    }


}
