package fr.epita.yeea2.service;

import fr.epita.yeea2.constant.EmailConstant;
import fr.epita.yeea2.dto.BurnoutStatusDailyResponse;
import fr.epita.yeea2.dto.BurnoutStatusResponse;
import fr.epita.yeea2.dto.SumarizationDto;
import fr.epita.yeea2.entity.AppUser;
import fr.epita.yeea2.constant.RecoveryRecommendationType;
import fr.epita.yeea2.entity.TaskStatus;
import fr.epita.yeea2.entity.WorkingHistory;
import fr.epita.yeea2.repository.TaskStatusRepository;
import fr.epita.yeea2.repository.UserRepository;
import fr.epita.yeea2.repository.WorkingHistoryRepository;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
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
    private final WorkingHistoryRepository workingHistoryRepository;
    private final MailService mailService;

    public BurnoutStatusDailyResponse calculateDailyBurnoutScore(AppUser user) {
        String userId = user.getId();

        LocalDate today = LocalDate.now(ZoneId.of("Europe/Paris"));

        WorkingHistory workingHistory = workingHistoryRepository.findByUserIdAndDay(userId, today.toString())
                .orElse(null);

        if (workingHistory == null || workingHistory.getSumarization() == null) {
            return BurnoutStatusDailyResponse.builder()
                    .userId(userId)
                    .day(today.toString())
                    .userEmail(user.getEmail())
                    .burnoutScore(0)
                    .riskLevel("Normal")
                    .recommendationMessage("No data for today.")
                    .extendedWorkSessions(0)
                    .lackOfBreaks(0)
                    .nightWork(0)
                    .todayWorkload(0)
                    .frequentContextSwitching(0)
                    .build();
        }

        var sum = workingHistory.getSumarization();

        int extendedWorkSessionsPoint = sum.getWorkingDuration() >= 1 * 60 * 60 * 1000L ? 10 : 0;
        int lackOfBreaksPoint = sum.getNumberOfBreaks() < 3 ? 10 : 0;
        int nightWorkPoint = 0; // 미구현
        int todayWorkloadPoint = sum.getWorkingDuration() >= 2 * 60 * 60 * 1000L ? 10 : 0;
        int contextSwitchPoint = sum.getContextSwitching() >= 1 ? 10 : 0;

        int score = extendedWorkSessionsPoint + lackOfBreaksPoint + nightWorkPoint + todayWorkloadPoint + contextSwitchPoint;
        score = Math.min(score, 100);

        String level = score < 40 ? "Normal" : (score < 70 ? "Caution" : "High");
        RecoveryRecommendationType recType = mapRiskLevelToRecovery(level);

        return BurnoutStatusDailyResponse.builder()
                .userId(userId)
                .day(today.toString())
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


    public List<BurnoutStatusDailyResponse> calculateWeeklyBurnoutScore() {
        String email = getEmailFromSecurityContext();
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String userId = user.getId();
        List<BurnoutStatusDailyResponse> weeklyResponses = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate targetDate = LocalDate.now().minusDays(i);
            String dayString = targetDate.toString();

            WorkingHistory workingHistory = workingHistoryRepository.findByUserIdAndDay(userId, dayString).orElse(null);

            long workingDuration = 0;
            long breakDuration = 0;
            int numberOfBreaks = 0;
            int contextSwitching = 0;

            if (workingHistory != null && workingHistory.getSumarization() != null) {
                workingDuration = workingHistory.getSumarization().getWorkingDuration();
                breakDuration = workingHistory.getSumarization().getBreakDuration();
                numberOfBreaks = workingHistory.getSumarization().getNumberOfBreaks();
                contextSwitching = workingHistory.getSumarization().getContextSwitching();
            }

            int workloadPoint = (workingDuration <= 480 * 60 * 1000L) ? 0 :
                    (workingDuration <= 600 * 60 * 1000L) ? 10 : 20;

            int nightWorkPoint = 0;

            int contextSwitchPoint = (contextSwitching > 5) ? 20 : 0;
            int breakPoint = (breakDuration < 30 * 60 * 1000L) ? 20 : 0;

            int score = workloadPoint + breakPoint + nightWorkPoint + contextSwitchPoint;
            score = Math.min(score, 100);

            String level = (score < 40) ? "Normal" : (score < 70 ? "Caution" : "High");
            RecoveryRecommendationType recType = mapRiskLevelToRecovery(level);

            BurnoutStatusDailyResponse response = BurnoutStatusDailyResponse.builder()
                    .userId(userId)
                    .userEmail(user.getEmail())
                    .burnoutScore(score)
                    .riskLevel(level)
                    .recommendationMessage(recType.getMessage())
                    .extendedWorkSessions(workloadPoint)
                    .lackOfBreaks(breakPoint)
                    .nightWork(nightWorkPoint)
                    .todayWorkload(workloadPoint)
                    .frequentContextSwitching(contextSwitchPoint)
                    .build();

            weeklyResponses.add(response);
        }

        return weeklyResponses;
    }


    public BurnoutStatusDailyResponse calculateDailyBurnoutByTime() {
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

    public List<BurnoutStatusResponse> calculateWeeklyBurnoutByTime() {
        String email = getEmailFromSecurityContext();
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String userId = user.getId();

        List<BurnoutStatusResponse> weeklyResponses = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate targetDate = LocalDate.now().minusDays(i);
            LocalDateTime startOfDay = targetDate.atStartOfDay();
            LocalDateTime endOfDay = targetDate.atTime(LocalTime.MAX);

            List<TaskStatus> dailyTasks = taskStatusRepository.findByUserIdAndStartTimeBetween(userId, startOfDay, endOfDay);

            long totalWorkedMinutes = dailyTasks.stream()
                    .filter(t -> t.getEndTime() != null && !t.isBreak())
                    .mapToLong(t -> Duration.between(t.getStartTime(), t.getEndTime()).toMinutes())
                    .sum();

            long totalBreakMinutes = dailyTasks.stream()
                    .filter(TaskStatus::isBreak)
                    .mapToLong(t -> Duration.between(t.getStartTime(), t.getEndTime()).toMinutes())
                    .sum();

            long nightWorkCount = dailyTasks.stream()
                    .filter(t -> t.getStartTime() != null && t.getStartTime().getHour() >= 20)
                    .count();

            long shortTasks = dailyTasks.stream()
                    .filter(t -> !t.isBreak())
                    .filter(t -> t.getStartTime() != null && t.getEndTime() != null)
                    .filter(t -> Duration.between(t.getStartTime(), t.getEndTime()).toMinutes() < 15)
                    .count();

            int workloadPoint = (totalWorkedMinutes <= 480) ? 0 : (totalWorkedMinutes <= 600) ? 10 : 20;
            int nightWorkPoint = (nightWorkCount == 0) ? 0 : (nightWorkCount <= 2) ? 10 : 20;
            int contextSwitchPoint = shortTasks > 5 ? 20 : 0;
            int breakPoint = totalBreakMinutes < 30 ? 20 : 0;

            int score = workloadPoint + breakPoint + nightWorkPoint + contextSwitchPoint;
            score = Math.min(score, 100);

            String level = (score < 40) ? "Normal" : (score < 70 ? "Caution" : "High");
            RecoveryRecommendationType recType = mapRiskLevelToRecovery(level);

            BurnoutStatusResponse response = BurnoutStatusResponse.builder()
                    .userId(userId)
                    .userEmail(user.getEmail())
                    .day(targetDate.toString())
                    .burnoutScore(score)
                    .riskLevel(level)
                    .recommendationMessage(recType.getMessage())
                    .build();

            weeklyResponses.add(response);
        }

        return weeklyResponses;
    }

    private RecoveryRecommendationType mapRiskLevelToRecovery(String level) {
        return switch (level) {
            case "High" -> RecoveryRecommendationType.SLEEP;
            case "Caution" -> RecoveryRecommendationType.STRETCH;
            default -> RecoveryRecommendationType.NORMAL;
        };
    }

    public String getEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    public void createMockTodayTasks() {
        String email = getEmailFromSecurityContext();
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String userId = user.getId();
        String today = LocalDate.now().toString();

        WorkingHistory workingHistory = new WorkingHistory();
        workingHistory.setUserId(userId);
        workingHistory.setDay(today);
        workingHistory.setWorking(false);
        workingHistory.setTaskChangedHistory(new ArrayList<>());
        workingHistory.setWorkTimeHistory(new ArrayList<>());

        SumarizationDto sum = new SumarizationDto();
        sum.setWorkingDuration(3 * 60 * 60 * 1000L);
        sum.setBreakDuration(30 * 60 * 1000L);
        sum.setContextSwitching(6);
        sum.setNumberOfBreaks(1);

        workingHistory.setSumarization(sum);

        workingHistoryRepository.save(workingHistory);
    }

    public void createMockWeeklyTasks() {
        String email = getEmailFromSecurityContext();
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String userId = user.getId();

        LocalDate baseDate = LocalDate.of(2025, 7, 24);
        LocalDate monday = baseDate.with(DayOfWeek.MONDAY);

        List<WorkingHistory> weeklyMockHistories = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate workDay = monday.plusDays(i);

            WorkingHistory wh = new WorkingHistory();
            wh.setUserId(userId);
            wh.setDay(workDay.toString());
            wh.setWorking(false);
            wh.setTaskChangedHistory(new ArrayList<>());
            wh.setWorkTimeHistory(new ArrayList<>());

            SumarizationDto sum = new SumarizationDto();

            switch (i) {
                case 0 -> {
                    sum.setWorkingDuration(14 * 60 * 60 * 1000L);
                    sum.setBreakDuration(10 * 60 * 1000L);
                    sum.setNumberOfBreaks(0);
                    sum.setContextSwitching(10);
                }
                case 1 -> {
                    sum.setWorkingDuration(10 * 60 * 60 * 1000L);
                    sum.setBreakDuration(20 * 60 * 1000L);
                    sum.setNumberOfBreaks(1);
                    sum.setContextSwitching(15);
                }
                case 2 -> {
                    sum.setWorkingDuration(6 * 60 * 60 * 1000L);
                    sum.setBreakDuration(15 * 60 * 1000L);
                    sum.setNumberOfBreaks(0);
                    sum.setContextSwitching(30);
                }
                case 3 -> {
                    sum.setWorkingDuration(6 * 60 * 60 * 1000L);
                    sum.setBreakDuration(15 * 60 * 1000L);
                    sum.setNumberOfBreaks(3);
                    sum.setContextSwitching(0);
                }
                case 4 -> {
                    sum.setWorkingDuration(8 * 60 * 60 * 1000L);
                    sum.setBreakDuration(30 * 60 * 1000L);
                    sum.setNumberOfBreaks(2);
                    sum.setContextSwitching(3);
                }
                case 5 -> {
                    sum.setWorkingDuration(9 * 60 * 60 * 1000L);
                    sum.setBreakDuration(0);
                    sum.setNumberOfBreaks(0);
                    sum.setContextSwitching(6);
                }
                case 6 -> {
                    sum.setWorkingDuration(6 * 60 * 60 * 1000L);
                    sum.setBreakDuration(15 * 60 * 1000L);
                    sum.setNumberOfBreaks(0);
                    sum.setContextSwitching(0);
                }
            }

            wh.setSumarization(sum);
            weeklyMockHistories.add(wh);
        }

        workingHistoryRepository.saveAll(weeklyMockHistories);
    }

    //Cron Job running daily
//    @Scheduled(cron = "${scheduler.daily-task.cron}")
//    @PostConstruct
    public void runDailyTask() {
        System.out.println("⏰ Running daily task at 23:59...");
        try {
        List<AppUser> users = userRepository.findAll();
        users.forEach(user -> {
            BurnoutStatusDailyResponse dataResponse = this.calculateDailyBurnoutScore(user);
            if (dataResponse != null) {
                String htmlBody = String.format(
                        EmailConstant.htmlEmailTemplate,
                        dataResponse.getDay(),
                        user.getFirstName(),
                        dataResponse.getBurnoutScore(),
                        dataResponse.getRiskLevel(),
                        dataResponse.getRecommendationMessage(),
                        dataResponse.getExtendedWorkSessions(),
                        dataResponse.getLackOfBreaks(),
                        dataResponse.getNightWork(),
                        dataResponse.getTodayWorkload(),
                        dataResponse.getFrequentContextSwitching()
                );

                try {
                    mailService.sendSimpleEmail(user.getEmail(), EmailConstant.SYSTEM_NAME, htmlBody);
                } catch (MessagingException e) {
                   e.printStackTrace();
                }

            }
        });
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
//                    throw new RuntimeException(e);
        }
    }
}
