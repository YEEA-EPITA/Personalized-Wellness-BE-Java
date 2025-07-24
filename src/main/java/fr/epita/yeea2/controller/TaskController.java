package fr.epita.yeea2.controller;

import fr.epita.yeea2.dto.ApiResponse;
import fr.epita.yeea2.entity.Task;
import fr.epita.yeea2.entity.WorkingHistory;
import fr.epita.yeea2.service.WorkingHistoryService;
import fr.epita.yeea2.service.JwtService;
import fr.epita.yeea2.service.TaskService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final JwtService jwtService;
    private final WorkingHistoryService workingHistoryService;

    @GetMapping("/changeWorkingStatus")
    public ResponseEntity<ApiResponse> changeWorkingStatus(HttpServletRequest request){
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse<?> errorResponse = new ApiResponse<>(
                    401,
                    HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                    null
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);
        String userId = jwtService.extractUserId(token);
        WorkingHistory workingHistory = workingHistoryService.changeWorkingHistory(userId);
        return ResponseEntity.ok(new ApiResponse<>(200, "Working status changed successfully", workingHistory));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Task>> createTask(@RequestBody Task task) {
        Task created = taskService.createTask(task);
        return ResponseEntity.ok(new ApiResponse<>(200, "Task created", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Task>> getTask(@PathVariable String id) {
        return taskService.getTaskById(id)
                .map(task -> ResponseEntity.ok(new ApiResponse<>(200, "Task found", task)))
                .orElse(ResponseEntity.status(404).body(new ApiResponse<>(404, "Task not found", null)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Task>>> getAllTasks() {
        List<Task> list = taskService.getAllTasks();
        return ResponseEntity.ok(new ApiResponse<>(200, "Task list", list));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Task>> updateTask(@PathVariable String id, @RequestBody Task task) {
        return taskService.updateTask(id, task)
                .map(updated -> ResponseEntity.ok(new ApiResponse<>(200, "Task updated", updated)))
                .orElse(ResponseEntity.status(404).body(new ApiResponse<>(404, "Task not found", null)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteTask(@PathVariable String id) {
        boolean deleted = taskService.deleteTask(id);
        if (deleted) {
            return ResponseEntity.ok(new ApiResponse<>(200, "Task deleted", "Deleted task with id: " + id));
        } else {
            return ResponseEntity.status(404).body(new ApiResponse<>(404, "Task not found", null));
        }
    }
}
