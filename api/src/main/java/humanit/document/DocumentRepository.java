package humanit.document;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByClientId(Long clientId);

    Optional<Document> findByIdAndClientId(Long id, Long clientId);

    boolean existsByClientIdAndNumber(Long clientId, String number);

    boolean existsByClientIdAndNumberAndIdNot(Long clientId, String number, Long id);
}
