package humanit.client;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import humanit.client.dto.ClientSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientRepository extends JpaRepository<Client, Long> {
    boolean existsByEmail(String email);

    boolean existsByTaxIdentifier(String taxIdentifier);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByTaxIdentifierAndIdNot(String taxIdentifier, Long id);

    @Query("""
            SELECT DISTINCT c
            FROM Client c
            LEFT JOIN FETCH c.documents
            WHERE c.id = :id
            """)
    Optional<Client> findByIdWithDocuments(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT c
            FROM Client c
            LEFT JOIN FETCH c.documents
            WHERE c.id IN :ids
            """)
    List<Client> findAllByIdWithDocuments(@Param("ids") Collection<Long> ids);

    // Used by GET /clients to include documentCount without loading each client's documents.
    @Query(value = """
            SELECT NEW humanit.client.dto.ClientSummaryResponse(
                c.id,
                c.firstName,
                c.lastName,
                c.taxIdentifier,
                c.email,
                c.phoneNumber,
                COUNT(d.id)
            )
            FROM Client c
            LEFT JOIN c.documents d
            GROUP BY c.id, c.firstName, c.lastName, c.taxIdentifier, c.email, c.phoneNumber
            """, countQuery = "select count(c) from Client c")
    Page<ClientSummaryResponse> findClientSummaries(Pageable pageable);
}
