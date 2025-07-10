package fr.epita.yeea2.service;

import fr.epita.yeea2.entity.Task;
import fr.epita.yeea2.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    public Task createTask(Task task) {
        return taskRepository.save(task);
    }

    public Optional<Task> getTaskById(String id) {
        return taskRepository.findById(id);
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Optional<Task> updateTask(String id, Task task) {
        return taskRepository.findById(id).map(existing -> {
            existing.setName(task.getName());
            existing.setStartDate(task.getStartDate());
            existing.setEndDate(task.getEndDate());
            existing.setDescription(task.getDescription());
            return taskRepository.save(existing);
        });
    }

    public boolean deleteTask(String id) {
        if (!taskRepository.existsById(id)) return false;
        taskRepository.deleteById(id);
        return true;
    }
}
