package humanit.document.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "Paginated document summaries response.")
public record DocumentPageResponse(
    @ArraySchema(
            arraySchema = @Schema(description = "Documents in the current page."),
            schema = @Schema(implementation = DocumentSummaryResponse.class))
        List<DocumentSummaryResponse> content,
    @Schema(description = "Zero-based page number.", example = "0") int page,
    @Schema(description = "Requested page size.", example = "20") int size,
    @Schema(description = "Number of documents returned in this page.", example = "10")
        int numberOfElements,
    @Schema(description = "Total number of matching documents.", example = "42") long totalElements,
    @Schema(description = "Total number of pages.", example = "3") int totalPages,
    @Schema(description = "Whether this is the first page.", example = "true") boolean first,
    @Schema(description = "Whether this is the last page.", example = "false") boolean last,
    @Schema(description = "Whether this page has no content.", example = "false") boolean empty) {

  public static DocumentPageResponse from(Page<DocumentSummaryResponse> page) {
    return new DocumentPageResponse(
        List.copyOf(page.getContent()),
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
