package fr.epita.yeea2.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "task_status")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatus {
    @Id
    private String id;
    private String userId;
    private String title;
    private String status; // "TODO", "IN_PROGRESS", "DONE"
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean isBreak;
}
