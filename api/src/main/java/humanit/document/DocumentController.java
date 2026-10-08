package humanit.document;

import java.net.URI;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import humanit.config.OpenApiExamples;
import humanit.config.OpenApiConfig;
import humanit.document.dto.CreateDocumentRequest;
import humanit.document.dto.DocumentResponse;
import humanit.document.dto.DocumentSummaryResponse;
import humanit.document.dto.UpdateDocumentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Tag(name = "Documents", description = "Manage documents belonging to a client.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@ApiResponse(responseCode = "401", description = "Missing or invalid bearer token", content = @Content)
@Validated
@RestController
@RequestMapping("/api/v1/clients/{clientId}/documents")
public class DocumentController {
    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Operation(summary = "Create a document", description = "Creates a document under the selected client.")
    @ApiResponse(responseCode = "201", description = "Document created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DocumentResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class), examples = @ExampleObject(value = OpenApiExamples.VALIDATION_PROBLEM)))
    @ApiResponse(responseCode = "404", description = "Client not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class), examples = @ExampleObject(value = OpenApiExamples.CLIENT_NOT_FOUND_PROBLEM)))
    @ApiResponse(responseCode = "409", description = "Document number already exists for the client", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class), examples = @ExampleObject(value = OpenApiExamples.DOCUMENT_CONFLICT_PROBLEM)))
    @PostMapping
    public ResponseEntity<DocumentResponse> createDocument(
            @PathVariable @Positive Long clientId,
            @Valid @RequestBody CreateDocumentRequest req) {
        DocumentResponse documentResponse = documentService.createDocument(clientId, req);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(documentResponse.id())
                .toUri();

        return ResponseEntity.created(location).body(documentResponse);
    }

    @Operation(summary = "Get a document")
    @ApiResponse(responseCode = "404", description = "Client or document not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class), examples = @ExampleObject(value = OpenApiExamples.DOCUMENT_NOT_FOUND_PROBLEM)))
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(
            @PathVariable @Positive Long clientId,
            @PathVariable @Positive Long id) {
        DocumentResponse documentResponse = documentService.getDocument(clientId, id);
        return ResponseEntity.ok(documentResponse);
    }

    @Operation(summary = "Update a document")
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class), examples = @ExampleObject(value = OpenApiExamples.VALIDATION_PROBLEM)))
    @ApiResponse(responseCode = "404", description = "Client or document not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class), examples = @ExampleObject(value = OpenApiExamples.DOCUMENT_NOT_FOUND_PROBLEM)))
    @ApiResponse(responseCode = "409", description = "Document number already exists for the client", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class), examples = @ExampleObject(value = OpenApiExamples.DOCUMENT_CONFLICT_PROBLEM)))
    @PutMapping("/{id}")
    public ResponseEntity<DocumentResponse> updateDocument(
            @PathVariable @Positive Long clientId,
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateDocumentRequest req) {
        DocumentResponse documentResponse = documentService.updateDocument(clientId, id, req);
        return ResponseEntity.ok(documentResponse);
    }

    @Operation(summary = "Delete a document")
    @ApiResponse(responseCode = "204", description = "Document deleted", content = @Content)
    @ApiResponse(responseCode = "404", description = "Client or document not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class), examples = @ExampleObject(value = OpenApiExamples.DOCUMENT_NOT_FOUND_PROBLEM)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable @Positive Long clientId,
            @PathVariable @Positive Long id) {
        documentService.deleteDocument(clientId, id);
        return ResponseEntity.noContent().build();
    }

    // TODO add config to cap page size
    // TODO create a pagination response DTO
    @Operation(summary = "List documents", description = "Returns paginated documents for the selected client.")
    @ApiResponse(responseCode = "400", description = "Invalid pagination or sorting request", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class), examples = @ExampleObject(value = OpenApiExamples.VALIDATION_PROBLEM)))
    @ApiResponse(responseCode = "404", description = "Client not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class), examples = @ExampleObject(value = OpenApiExamples.CLIENT_NOT_FOUND_PROBLEM)))
    @GetMapping
    public ResponseEntity<Page<DocumentSummaryResponse>> getDocuments(
            @PathVariable @Positive Long clientId,
            @ParameterObject @PageableDefault(size = 20, sort = "expirationDate") Pageable pageable) {
        return ResponseEntity.ok(documentService.getDocuments(clientId, pageable));
    }
}
