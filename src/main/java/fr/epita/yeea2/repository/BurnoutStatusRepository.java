package fr.epita.yeea2.repository;

import fr.epita.yeea2.entity.BurnoutStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BurnoutStatusRepository extends MongoRepository<BurnoutStatus, String> {
    Optional<BurnoutStatus> findFirstByUserIdOrderByCalculatedDateDesc(String userId);
}
