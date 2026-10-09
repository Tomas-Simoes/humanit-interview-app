package humanit.client.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(
    description =
        "Paginated clients response. Content contains client summaries unless includeDocuments is true.")
public record ClientPageResponse(
    @ArraySchema(
            arraySchema = @Schema(description = "Clients in the current page."),
            schema = @Schema(oneOf = {ClientSummaryResponse.class, ClientResponse.class}))
        List<?> content,
    @Schema(description = "Whether each client includes full document data.", example = "false")
        boolean includeDocuments,
    @Schema(description = "Zero-based page number.", example = "0") int page,
    @Schema(description = "Requested page size.", example = "20") int size,
    @Schema(description = "Number of clients returned in this page.", example = "10")
        int numberOfElements,
    @Schema(description = "Total number of matching clients.", example = "42") long totalElements,
    @Schema(description = "Total number of pages.", example = "3") int totalPages,
    @Schema(description = "Whether this is the first page.", example = "true") boolean first,
    @Schema(description = "Whether this is the last page.", example = "false") boolean last,
    @Schema(description = "Whether this page has no content.", example = "false") boolean empty) {

  public static ClientPageResponse summaries(Page<ClientSummaryResponse> page) {
    return from(page, false);
  }

  public static ClientPageResponse withDocuments(Page<ClientResponse> page) {
    return from(page, true);
  }

  private static ClientPageResponse from(Page<?> page, boolean includeDocuments) {
    return new ClientPageResponse(
        List.copyOf(page.getContent()),
        includeDocuments,
        page.getNumber(),
        page.getSize(),
        page.getNumberOfElements(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isFirst(),
        page.isLast(),
        page.isEmpty());
  }
}
