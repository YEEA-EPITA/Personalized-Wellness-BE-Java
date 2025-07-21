package fr.epita.yeea2.burnout;

import fr.epita.yeea2.dto.BurnoutStatusDailyResponse;
import fr.epita.yeea2.dto.BurnoutStatusResponse;
import fr.epita.yeea2.entity.AppUser;
import fr.epita.yeea2.entity.TaskStatus;
import fr.epita.yeea2.repository.TaskStatusRepository;
import fr.epita.yeea2.repository.UserRepository;
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


import java.time.LocalDateTime;
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
    private UserRepository userRepository;

    @BeforeEach
    void setupSecurityContext() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "test@email.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCalculateDailyBurnoutScore() {
        // Given
        AppUser user = userRepository.findByEmail("test@email.com")
                .orElseThrow(() -> new RuntimeException("Test user not found"));

        LocalDateTime now = LocalDateTime.now();
        taskStatusRepository.saveAll(List.of(
                TaskStatus.builder().userId(user.getId()).startTime(now.minusHours(4)).endTime(now.minusHours(1)).isBreak(false).status("DONE").build(),
                TaskStatus.builder().userId(user.getId()).startTime(now.minusMinutes(40)).endTime(now.minusMinutes(10)).isBreak(true).status("DONE").build(),
                TaskStatus.builder().userId(user.getId()).startTime(now.withHour(21)).endTime(now.withHour(22)).isBreak(false).status("DONE").build()
        ));

        // When
        BurnoutStatusDailyResponse result = burnoutCalculatorService.calculateDailyBurnoutScore();

        // Then
        assertThat(result.getBurnoutScore()).isGreaterThan(0);
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
        AppUser user = userRepository.findByEmail("test@email.com")
                .orElseThrow(() -> new RuntimeException("Test user not found"));

        burnoutCalculatorService.createMockWeeklyTasks();

        List<BurnoutStatusResponse> results = burnoutCalculatorService.calculateWeeklyBurnoutScore();

        assertThat(results).isNotEmpty();
        for (BurnoutStatusResponse result : results) {
            assertThat(result.getBurnoutScore()).isGreaterThanOrEqualTo(0);
            assertThat(result.getUserId()).isEqualTo(user.getId());
            assertThat(result.getRiskLevel()).isIn("Normal", "Caution", "High");
        }
    }
}
