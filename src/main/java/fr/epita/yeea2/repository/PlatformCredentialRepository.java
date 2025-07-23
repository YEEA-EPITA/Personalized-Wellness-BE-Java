package fr.epita.yeea2.repository;

import fr.epita.yeea2.entity.PlatformCredential;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformCredentialRepository extends MongoRepository<PlatformCredential, String> {

    Optional<PlatformCredential> findByPlatformEmailAndTypeAndConnectorId(String email, String type, Object connectorId);

    Optional<List<PlatformCredential>> findAllByEmail(String email);

    void deleteAllByConnectorId(String connectorId);
}
