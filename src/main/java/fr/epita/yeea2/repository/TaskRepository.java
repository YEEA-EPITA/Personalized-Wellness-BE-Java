package fr.epita.yeea2.repository;

import fr.epita.yeea2.entity.Task;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TaskRepository extends MongoRepository<Task, String> {
}
