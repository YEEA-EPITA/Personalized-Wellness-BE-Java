package fr.epita.yeea2.controller;

import fr.epita.yeea2.dto.ApiResponse;
import fr.epita.yeea2.entity.Task;
import fr.epita.yeea2.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

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
