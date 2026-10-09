package humanit.document;

import humanit.config.OpenApiConfig;
import humanit.config.OpenApiExamples;
import humanit.document.dto.CreateDocumentRequest;
import humanit.document.dto.DocumentPageResponse;
import humanit.document.dto.DocumentResponse;
import humanit.document.dto.DocumentSummaryResponse;
import humanit.document.dto.UpdateDocumentRequest;
import humanit.pagination.PageResponse;
import humanit.pagination.PageableGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.Set;
import org.springdoc.core.annotations.ParameterObject;
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

@Tag(name = "Documents", description = "List and manage client documents.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@ApiResponse(
    responseCode = "401",
    description = "Missing or invalid bearer token",
    content = @Content)
@Validated
@RestController
@RequestMapping("/api/v1")
public class DocumentController {
  private static final Set<String> ALLOWED_SORT_FIELDS =
      Set.of("id", "number", "description", "expirationDate");

  private final DocumentService documentService;
  private final PageableGuard pageableGuard;

  public DocumentController(DocumentService documentService, PageableGuard pageableGuard) {
    this.documentService = documentService;
    this.pageableGuard = pageableGuard;
  }

  @Operation(
      summary = "Create a document",
      description = "Creates a document under the selected client.")
  @ApiResponse(
      responseCode = "201",
      description = "Document created",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = DocumentResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Validation failed",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class),
              examples = @ExampleObject(value = OpenApiExamples.VALIDATION_PROBLEM)))
  @ApiResponse(
      responseCode = "404",
      description = "Client not found",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class),
              examples = @ExampleObject(value = OpenApiExamples.CLIENT_NOT_FOUND_PROBLEM)))
  @ApiResponse(
      responseCode = "409",
      description = "Document number already exists for the client",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class),
              examples = @ExampleObject(value = OpenApiExamples.DOCUMENT_CONFLICT_PROBLEM)))
  @PostMapping("/clients/{clientId}/documents")
  public ResponseEntity<DocumentResponse> createDocument(
      @PathVariable @Positive Long clientId, @Valid @RequestBody CreateDocumentRequest req) {
    DocumentResponse documentResponse = documentService.createDocument(clientId, req);

    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(documentResponse.id())
            .toUri();

    return ResponseEntity.created(location).body(documentResponse);
  }

  @Operation(summary = "Get a document")
  @ApiResponse(
      responseCode = "404",
      description = "Client or document not found",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class),
              examples = @ExampleObject(value = OpenApiExamples.DOCUMENT_NOT_FOUND_PROBLEM)))
  @GetMapping("/clients/{clientId}/documents/{id}")
  public ResponseEntity<DocumentResponse> getDocument(
      @PathVariable @Positive Long clientId, @PathVariable @Positive Long id) {
    DocumentResponse documentResponse = documentService.getDocument(clientId, id);
    return ResponseEntity.ok(documentResponse);
  }

  @Operation(summary = "Update a document")
  @ApiResponse(
      responseCode = "400",
      description = "Validation failed",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class),
              examples = @ExampleObject(value = OpenApiExamples.VALIDATION_PROBLEM)))
  @ApiResponse(
      responseCode = "404",
      description = "Client or document not found",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class),
              examples = @ExampleObject(value = OpenApiExamples.DOCUMENT_NOT_FOUND_PROBLEM)))
  @ApiResponse(
      responseCode = "409",
      description = "Document number already exists for the client",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class),
              examples = @ExampleObject(value = OpenApiExamples.DOCUMENT_CONFLICT_PROBLEM)))
  @PutMapping("/clients/{clientId}/documents/{id}")
  public ResponseEntity<DocumentResponse> updateDocument(
      @PathVariable @Positive Long clientId,
      @PathVariable @Positive Long id,
      @Valid @RequestBody UpdateDocumentRequest req) {
    DocumentResponse documentResponse = documentService.updateDocument(clientId, id, req);
    return ResponseEntity.ok(documentResponse);
  }

  @Operation(summary = "Delete a document")
  @ApiResponse(responseCode = "204", description = "Document deleted", content = @Content)
  @ApiResponse(
      responseCode = "404",
      description = "Client or document not found",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class),
              examples = @ExampleObject(value = OpenApiExamples.DOCUMENT_NOT_FOUND_PROBLEM)))
  @DeleteMapping("/clients/{clientId}/documents/{id}")
  public ResponseEntity<Void> deleteDocument(
      @PathVariable @Positive Long clientId, @PathVariable @Positive Long id) {
    documentService.deleteDocument(clientId, id);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "List all documents",
      description = "Returns paginated document summaries across all clients.")
  @ApiResponse(
      responseCode = "200",
      description = "Documents listed",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = DocumentPageResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Invalid pagination or sorting request",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class),
              examples = @ExampleObject(value = OpenApiExamples.VALIDATION_PROBLEM)))
  @GetMapping("/documents")
  public ResponseEntity<DocumentPageResponse> getDocuments(
      @ParameterObject @PageableDefault(size = 20, sort = "expirationDate") Pageable pageable) {
    pageableGuard.requireAllowedSort(pageable, ALLOWED_SORT_FIELDS);

    return ResponseEntity.ok(DocumentPageResponse.from(documentService.getDocuments(pageable)));
  }

  @Operation(
      summary = "List client documents",
      description = "Returns paginated documents for the selected client.")
  @ApiResponse(
      responseCode = "400",
      description = "Invalid pagination or sorting request",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class),
              examples = @ExampleObject(value = OpenApiExamples.VALIDATION_PROBLEM)))
  @ApiResponse(
      responseCode = "404",
      description = "Client not found",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class),
              examples = @ExampleObject(value = OpenApiExamples.CLIENT_NOT_FOUND_PROBLEM)))
  @GetMapping("/clients/{clientId}/documents")
  public ResponseEntity<PageResponse<DocumentSummaryResponse>> getClientDocuments(
      @PathVariable @Positive Long clientId,
      @ParameterObject @PageableDefault(size = 20, sort = "expirationDate") Pageable pageable) {
    pageableGuard.requireAllowedSort(pageable, ALLOWED_SORT_FIELDS);

    return ResponseEntity.ok(PageResponse.from(documentService.getDocuments(clientId, pageable)));
  }
}
