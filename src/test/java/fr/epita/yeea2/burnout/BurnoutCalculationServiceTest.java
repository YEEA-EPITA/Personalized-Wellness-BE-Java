package fr.epita.yeea2.burnout;

import fr.epita.yeea2.dto.BurnoutStatusDailyResponse;
import fr.epita.yeea2.dto.BurnoutStatusResponse;
import fr.epita.yeea2.dto.SumarizationDto;
import fr.epita.yeea2.entity.AppUser;
import fr.epita.yeea2.entity.TaskStatus;
import fr.epita.yeea2.entity.WorkingHistory;
import fr.epita.yeea2.repository.TaskStatusRepository;
import fr.epita.yeea2.repository.UserRepository;
import fr.epita.yeea2.repository.WorkingHistoryRepository;
import fr.epita.yeea2.service.BurnoutCalculatorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;


@SpringBootTest(classes = fr.epita.yeea2.Yeea2Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BurnoutCalculationServiceTest {

    @Autowired
    private BurnoutCalculatorService burnoutCalculatorService;

    @Autowired
    private TaskStatusRepository taskStatusRepository;

    @Autowired
    private WorkingHistoryRepository workingHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setupSecurityContext() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "test@email.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        if (userRepository.findByEmail("test@email.com").isEmpty()) {
            AppUser user = AppUser.builder()
                    .email("test@email.com")
                    .password("dummy")
                    .build();
            userRepository.save(user);
        }
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
//        taskStatusRepository.deleteAll();
//        workingHistoryRepository.deleteAll();
    }

    @Test
    void testCalculateDailyBurnoutScore() {
        // Given
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Paris"));

        AppUser user = userRepository.findByEmail("test@email.com")
                .orElseThrow(() -> new RuntimeException("Test user not found"));

        var summary = new fr.epita.yeea2.dto.SumarizationDto();
        summary.setWorkingDuration(3 * 60 * 60 * 1000L); // 3h
        summary.setBreakDuration(15 * 60 * 1000L);       // 15min
        summary.setNumberOfBreaks(1);
        summary.setContextSwitching(5);

        var history = new WorkingHistory();
        history.setUserId(user.getId());
        history.setDay(today.toString());
        history.setWorking(true);
        history.setSumarization(summary);
        history.setTaskChangedHistory(new ArrayList<>());
        history.setWorkTimeHistory(new ArrayList<>());

        workingHistoryRepository.save(history);

        // When
        BurnoutStatusDailyResponse result = burnoutCalculatorService.calculateDailyBurnoutScore(user);

        // Then
        assertThat(result.getUserId()).isEqualTo(user.getId());
        assertThat(result.getUserEmail()).isEqualTo(user.getEmail());
        assertThat(result.getBurnoutScore()).isBetween(0, 50); // expected score: 40
        assertThat(result.getRiskLevel()).isIn("Normal", "Caution", "High");
        assertThat(result.getRecommendationMessage()).isNotBlank();
        assertThat(result.getExtendedWorkSessions()).isEqualTo(10);
        assertThat(result.getLackOfBreaks()).isEqualTo(10);
        assertThat(result.getFrequentContextSwitching()).isEqualTo(10);
        assertThat(result.getTodayWorkload()).isEqualTo(10);
        assertThat(result.getNightWork()).isEqualTo(0); // 미구현
    }

    @Test
    void testCalculateWeeklyBurnoutScore() {
        AppUser user = userRepository.findByEmail("test@email.com")
                .orElseThrow(() -> new RuntimeException("Test user not found"));

        // Given
        for (int i = 0; i < 7; i++) {
            LocalDate date = LocalDate.now().minusDays(i);

            var summary = new fr.epita.yeea2.dto.SumarizationDto();
            summary.setWorkingDuration(9 * 60 * 60 * 1000L); // 9h
            summary.setBreakDuration(20 * 60 * 1000L);       // 20min
            summary.setNumberOfBreaks(2);
            summary.setContextSwitching(6);

            var history = new WorkingHistory();
            history.setUserId(user.getId());
            history.setDay(date.toString());
            history.setWorking(true);
            history.setSumarization(summary);
            history.setTaskChangedHistory(new ArrayList<>());
            history.setWorkTimeHistory(new ArrayList<>());

            workingHistoryRepository.save(history);
        }

        // When
        List<BurnoutStatusDailyResponse> resultList = burnoutCalculatorService.calculateWeeklyBurnoutScore();

        // Then
        assertThat(resultList).hasSize(7);
        for (BurnoutStatusDailyResponse result : resultList) {
            assertThat(result.getUserId()).isEqualTo(user.getId());
            assertThat(result.getUserEmail()).isEqualTo(user.getEmail());
            assertThat(result.getBurnoutScore()).isBetween(0, 100);
            assertThat(result.getRiskLevel()).isIn("Normal", "Caution", "High");
            assertThat(result.getTodayWorkload()).isIn(10, 20);
            assertThat(result.getLackOfBreaks()).isEqualTo(20); // < 30min
            assertThat(result.getFrequentContextSwitching()).isEqualTo(20); // > 5
            assertThat(result.getRecommendationMessage()).isNotBlank();
        }
    }

}
