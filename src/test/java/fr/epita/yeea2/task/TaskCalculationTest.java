package fr.epita.yeea2.task;

import fr.epita.yeea2.entity.TaskStatus;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

public class TaskCalculationTest {

    @Test
    void testBreakAndWorkTimeCalculation() {
        // Given (Mock data)
        LocalDateTime now = LocalDateTime.now();
        List<TaskStatus> tasks = List.of(
                // Task 3h
                TaskStatus.builder()
                        .startTime(now.minusHours(5))
                        .endTime(now.minusHours(2))
                        .isBreak(false)
                        .build(),

                // Break 30 min
                TaskStatus.builder()
                        .startTime(now.minusHours(1))
                        .endTime(now.minusMinutes(30))
                        .isBreak(true)
                        .build(),

                // Task 1h
                TaskStatus.builder()
                        .startTime(now.minusMinutes(25))
                        .endTime(now)
                        .isBreak(false)
                        .build()
        );

        // When
        long totalWorkedMinutes = tasks.stream()
                .filter(t -> t.getEndTime() != null && !t.isBreak())
                .mapToLong(t -> Duration.between(t.getStartTime(), t.getEndTime()).toMinutes())
                .sum();

        long totalBreakMinutes = tasks.stream()
                .filter(t -> t.getEndTime() != null && t.isBreak())
                .mapToLong(t -> Duration.between(t.getStartTime(), t.getEndTime()).toMinutes())
                .sum();

        // Then
        assertThat(totalWorkedMinutes).isEqualTo(180 + 25); // 3h + 25min
        assertThat(totalBreakMinutes).isEqualTo(30); // 30min
    }
}

