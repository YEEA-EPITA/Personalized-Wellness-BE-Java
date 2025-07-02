package fr.epita.yeea2.repository;

import fr.epita.yeea2.entity.PlatformCredential;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlatformCredentialRepository extends MongoRepository<PlatformCredential, String> {

    Optional<PlatformCredential> findByPlatformEmailAndType(String email, String type);
}
