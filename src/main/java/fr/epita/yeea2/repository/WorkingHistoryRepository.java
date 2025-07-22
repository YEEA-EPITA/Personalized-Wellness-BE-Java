package fr.epita.yeea2.repository;

import fr.epita.yeea2.entity.PlatformCredential;
import fr.epita.yeea2.entity.WorkingHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkingHistoryRepository extends MongoRepository<WorkingHistory, String> {

    Optional<WorkingHistory> findByUserIdAndDay(String userId, String day);
}
