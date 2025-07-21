package fr.epita.yeea2.repository;

import fr.epita.yeea2.entity.TaskStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskStatusRepository extends MongoRepository<TaskStatus, String> {
    List<TaskStatus> findByUserIdAndStartTimeBetween(String userId, LocalDateTime start, LocalDateTime end);
}
