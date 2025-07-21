package fr.epita.yeea2.controller;

import fr.epita.yeea2.dto.BurnoutStatusDailyResponse;
import fr.epita.yeea2.dto.BurnoutStatusResponse;
import fr.epita.yeea2.service.BurnoutCalculatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/burnout")
@RequiredArgsConstructor
public class BurnoutController {
    private final BurnoutCalculatorService burnoutCalculatorService;

    @GetMapping("/daily-status")
    public ResponseEntity<BurnoutStatusDailyResponse> getDailyBurnoutStatus() {
        BurnoutStatusDailyResponse status = burnoutCalculatorService.calculateDailyBurnoutScore();
        return ResponseEntity.ok(status);
    }

    @GetMapping("/weekly-status")
    public ResponseEntity<BurnoutStatusResponse> getBurnoutStatus() {
        BurnoutStatusResponse status = burnoutCalculatorService.calculateWeeklyBurnoutScore();
        return ResponseEntity.ok(status);
    }

    @PostMapping("/mock-task")
    public ResponseEntity<String> createMockTask() {
        burnoutCalculatorService.createMockTodayTasks();
        burnoutCalculatorService.createMockWeeklyTasks();
        return ResponseEntity.ok("Mock tasks created.");
    }

}
