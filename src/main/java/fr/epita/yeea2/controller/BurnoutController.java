package fr.epita.yeea2.controller;

import fr.epita.yeea2.dto.ApiResponse;
import fr.epita.yeea2.dto.BurnoutStatusDailyResponse;
import fr.epita.yeea2.dto.BurnoutStatusResponse;
import fr.epita.yeea2.entity.AppUser;
import fr.epita.yeea2.repository.UserRepository;
import fr.epita.yeea2.service.BurnoutCalculatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/burnout")
@RequiredArgsConstructor
public class BurnoutController {
    private final BurnoutCalculatorService burnoutCalculatorService;
    private final UserRepository userRepository;

    @GetMapping("/daily-status")
    public ResponseEntity<ApiResponse<BurnoutStatusDailyResponse>> getDailyBurnoutStatus() {
        try {
            String email = burnoutCalculatorService.getEmailFromSecurityContext();
            AppUser user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            BurnoutStatusDailyResponse status = burnoutCalculatorService.calculateDailyBurnoutScore(user);
            if (status != null) {
                return ResponseEntity.ok(new ApiResponse<>(200, "Daily burnout", status));
            } else  {
                return ResponseEntity.ok(new ApiResponse<>(200, "No working history for today", null));
            }
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(new ApiResponse<>(e.getStatusCode().value(), e.getReason(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "Unexpected error occurred", null));
        }
    }

    @GetMapping("/weekly-status")
    public ResponseEntity<ApiResponse<List<BurnoutStatusResponse>>> getWeeklyBurnoutStatus() {
        try {
            List<BurnoutStatusResponse> status = burnoutCalculatorService.calculateWeeklyBurnoutScore();

            if (status == null || status.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse<>(204, "No working history for this week", List.of()));
            }

            return ResponseEntity.ok(new ApiResponse<>(200, "Weekly burnout", status));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(new ApiResponse<>(e.getStatusCode().value(), e.getReason(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "Unexpected error occurred", null));
        }
    }

    @PostMapping("/mock-task")
    public ResponseEntity<ApiResponse<String>> createMockTask() {
        try {
            burnoutCalculatorService.createMockWeeklyTasks();
            return ResponseEntity.ok(new ApiResponse<>(200, "Mock tasks created", "Mock data inserted"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "Failed to create mock tasks", null));
        }
    }

    @GetMapping("/send-email")
    public ResponseEntity<ApiResponse<String>> sendEmail() {
        try {
            burnoutCalculatorService.runDailyTask();
            return ResponseEntity.ok(new ApiResponse<>(200, "Emails sended", ""));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "Failed to send emails", null));
        }
    }
}
