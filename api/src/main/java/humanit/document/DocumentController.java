package humanit.document;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import humanit.document.dto.CreateDocumentRequest;
import humanit.document.dto.DocumentResponse;
import humanit.document.dto.DocumentSummaryResponse;
import humanit.document.dto.UpdateDocumentRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/clients/{clientId}/documents")
public class DocumentController {
    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public ResponseEntity<DocumentResponse> createDocument(
            @PathVariable Long clientId,
            @Valid @RequestBody CreateDocumentRequest req) {
        DocumentResponse documentResponse = documentService.createDocument(clientId, req);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(documentResponse.id())
                .toUri();

        return ResponseEntity.created(location).body(documentResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable Long clientId, @PathVariable Long id) {
        DocumentResponse documentResponse = documentService.getDocument(clientId, id);
        return ResponseEntity.ok(documentResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentResponse> updateDocument(
            @PathVariable Long clientId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateDocumentRequest req) {
        DocumentResponse documentResponse = documentService.updateDocument(clientId, id, req);
        return ResponseEntity.ok(documentResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long clientId, @PathVariable Long id) {
        documentService.deleteDocument(clientId, id);
        return ResponseEntity.noContent().build();
    }

    // TODO add config to cap page size
    // TODO create a pagination response DTO
    @GetMapping
    public ResponseEntity<Page<DocumentSummaryResponse>> getDocuments(
            @PathVariable Long clientId,
            @PageableDefault(size = 20, sort = "expirationDate") Pageable pageable) {
        return ResponseEntity.ok(documentService.getDocuments(clientId, pageable));
    }
}
