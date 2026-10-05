package humanit.client;

import humanit.client.dto.ClientSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClientRepository extends JpaRepository<Client, Long> {
    boolean existsByEmail(String email);

    boolean existsByTaxIdentifier(String taxIdentifier);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByTaxIdentifierAndIdNot(String taxIdentifier, Long id);

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
