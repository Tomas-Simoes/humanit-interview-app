package humanit.document;

import java.util.List;
import java.util.Optional;

import humanit.document.dto.DocumentSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByClientId(Long clientId);

    Optional<Document> findByIdAndClientId(Long id, Long clientId);

    boolean existsByClientIdAndNumber(Long clientId, String number);

    boolean existsByClientIdAndNumberAndIdNot(Long clientId, String number, Long id);

    // Used by GET /clients/{clientId}/documents to keep list responses lightweight.
    @Query(value = """
            SELECT NEW humanit.document.dto.DocumentSummaryResponse(
                d.id,
                d.number,
                d.description,
                d.expirationDate,
                d.client.id
            )
            FROM Document d
            WHERE d.client.id = :clientId
            """, countQuery = "select count(d) from Document d where d.client.id = :clientId")
    Page<DocumentSummaryResponse> findDocumentSummariesByClientId(@Param("clientId") Long clientId, Pageable pageable);
}
