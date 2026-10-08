package humanit.client;

import java.net.URI;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import humanit.client.dto.ClientPageResponse;
import humanit.client.dto.ClientResponse;
import humanit.client.dto.CreateClientRequest;
import humanit.client.dto.UpdateClientRequest;
import humanit.config.OpenApiExamples;
import humanit.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Tag(name = "Clients", description = "Manage clients and their documents.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@ApiResponse(responseCode = "401", description = "Missing or invalid bearer token", content = @Content)
@Validated
@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {
    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @Operation(summary = "Create a client", description = "Creates a client. Optional documents can be created in the same request.")
    @ApiResponse(responseCode = "201", description = "Client created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClientResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class), examples = @ExampleObject(value = OpenApiExamples.VALIDATION_PROBLEM)))
    @ApiResponse(responseCode = "409", description = "Client email, tax identifier, or nested document number already exists", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class), examples = @ExampleObject(value = OpenApiExamples.CLIENT_CONFLICT_PROBLEM)))
    @PostMapping
    public ResponseEntity<ClientResponse> createClient(@Valid @RequestBody CreateClientRequest req) {
        ClientResponse clientResponse = clientService.createClient(req);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(clientResponse.id())
                .toUri();

        return ResponseEntity.created(location).body(clientResponse);
    }

    @Operation(summary = "Get a client")
    @ApiResponse(responseCode = "404", description = "Client not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class), examples = @ExampleObject(value = OpenApiExamples.CLIENT_NOT_FOUND_PROBLEM)))
    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClient(@PathVariable @Positive Long id) {
        ClientResponse clientResponse = clientService.getClient(id);
        return ResponseEntity.ok(clientResponse);
    }

    @Operation(summary = "Update a client")
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class), examples = @ExampleObject(value = OpenApiExamples.VALIDATION_PROBLEM)))
    @ApiResponse(responseCode = "404", description = "Client not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class), examples = @ExampleObject(value = OpenApiExamples.CLIENT_NOT_FOUND_PROBLEM)))
    @ApiResponse(responseCode = "409", description = "Client email or tax identifier already exists", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class), examples = @ExampleObject(value = OpenApiExamples.CLIENT_CONFLICT_PROBLEM)))
    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateClientRequest req) {
        ClientResponse clientResponse = clientService.updateClient(id, req);
        return ResponseEntity.ok(clientResponse);
    }

    @Operation(summary = "Delete a client")
    @ApiResponse(responseCode = "204", description = "Client deleted", content = @Content)
    @ApiResponse(responseCode = "404", description = "Client not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class), examples = @ExampleObject(value = OpenApiExamples.CLIENT_NOT_FOUND_PROBLEM)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable @Positive Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }

    // TODO add config to cap page size
    @Operation(summary = "List clients", description = "Returns paginated client summaries. Set includeDocuments=true to include each client's documents.")
    @ApiResponse(responseCode = "200", description = "Clients listed", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClientPageResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid pagination or sorting request", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class), examples = @ExampleObject(value = OpenApiExamples.VALIDATION_PROBLEM)))
    @GetMapping
    public ResponseEntity<ClientPageResponse> getClients(
            @Parameter(description = "Include full document data for each client.", example = "false") @RequestParam(defaultValue = "false") boolean includeDocuments,
            @ParameterObject @PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
        if (includeDocuments) {
            return ResponseEntity.ok(ClientPageResponse.withDocuments(clientService.getClientsWithDocuments(pageable)));
        }

        return ResponseEntity.ok(ClientPageResponse.summaries(clientService.getClients(pageable)));
    }
}
