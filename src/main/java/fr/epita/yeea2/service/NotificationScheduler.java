package fr.epita.yeea2.service;

import fr.epita.yeea2.repository.TaskStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationScheduler {
    private final TaskStatusRepository taskStatusRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 9 * * *")
    public void sendMorningReminder() {
        List<String> userIds = List.of("user1", "user2");
        userIds.forEach(userId -> {
            long todoCount = taskStatusRepository.findByUserIdAndStartTimeBetween(
                            userId, LocalDate.now().atStartOfDay(), LocalDate.now().atTime(LocalTime.MAX))
                    .stream().filter(t -> "TODO".equals(t.getStatus())).count();
            notificationService.send(userId, "You have " + todoCount + " tasks to complete today.");
        });
    }

    @Scheduled(cron = "0 0 18 * * *")
    public void sendEveningCheck() {
        List<String> userIds = List.of("user1", "user2");
        userIds.forEach(userId -> {
            long workedMinutes = taskStatusRepository.findByUserIdAndStartTimeBetween(
                            userId, LocalDate.now().atStartOfDay(), LocalDate.now().atTime(LocalTime.MAX))
                    .stream()
                    .filter(t -> t.getEndTime() != null && !t.isBreak())
                    .mapToLong(t -> Duration.between(t.getStartTime(), t.getEndTime()).toMinutes())
                    .sum();
            String message = "You have worked " + workedMinutes / 60 + " hours today. Please check if you are at risk of burnout.";
            notificationService.send(userId, message);
        });
    }
}
