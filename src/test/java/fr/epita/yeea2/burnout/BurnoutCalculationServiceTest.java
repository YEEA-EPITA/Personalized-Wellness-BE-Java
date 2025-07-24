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
        AppUser user = userRepository.findByEmail("test@email.com")
                .orElseThrow(() -> new RuntimeException("Test user not found"));

        WorkingHistory wh = new WorkingHistory();
        wh.setUserId(user.getId());
        wh.setDay(LocalDate.now().toString());
        wh.setWorking(false);
        wh.setTaskChangedHistory(new ArrayList<>());
        wh.setWorkTimeHistory(new ArrayList<>());

        SumarizationDto sum = new SumarizationDto();
        sum.setWorkingDuration(3 * 60 * 60 * 1000L); // 3시간
        sum.setBreakDuration(30 * 60 * 1000L); // 30분
        sum.setContextSwitching(6);
        sum.setNumberOfBreaks(1);

        wh.setSumarization(sum);
        workingHistoryRepository.save(wh);

        // When
        BurnoutStatusDailyResponse result = burnoutCalculatorService.calculateDailyBurnoutScore(user);

        // Then
        assertThat(result.getBurnoutScore()).isGreaterThanOrEqualTo(0);
        assertThat(result.getUserId()).isEqualTo(user.getId());
        assertThat(result.getRiskLevel()).isIn("Normal", "Caution", "High");
        assertThat(result.getExtendedWorkSessions()).isIn(0, 20);
        assertThat(result.getLackOfBreaks()).isIn(0, 10, 20);
        assertThat(result.getNightWork()).isIn(0, 10, 20);
        assertThat(result.getTodayWorkload()).isIn(0, 10, 20);
        assertThat(result.getFrequentContextSwitching()).isIn(0, 20);
    }

    @Test
    void testCalculateWeeklyBurnoutScore() {
        // Given
        AppUser user = userRepository.findByEmail("test@email.com")
                .orElseThrow(() -> new RuntimeException("Test user not found"));

        burnoutCalculatorService.createMockWeeklyTasks();

        // When
        List<BurnoutStatusResponse> results = burnoutCalculatorService.calculateWeeklyBurnoutScore();

        // Then
        assertThat(results).hasSize(7);
        for (BurnoutStatusResponse result : results) {
            assertThat(result.getUserId()).isEqualTo(user.getId());
            assertThat(result.getBurnoutScore()).isBetween(0, 100);
            assertThat(result.getRiskLevel()).isIn("Normal", "Caution", "High");
        }
    }
}
