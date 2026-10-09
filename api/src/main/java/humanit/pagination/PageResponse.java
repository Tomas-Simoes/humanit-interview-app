package humanit.pagination;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "Stable paginated response.")
public record PageResponse<T>(
    @Schema(description = "Items in the current page.") List<T> content,
    @Schema(description = "Zero-based page number.", example = "0") int page,
    @Schema(description = "Requested page size.", example = "20") int size,
    @Schema(description = "Number of items returned in this page.", example = "10")
        int numberOfElements,
    @Schema(description = "Total number of matching items.", example = "42") long totalElements,
    @Schema(description = "Total number of pages.", example = "3") int totalPages,
    @Schema(description = "Whether this is the first page.", example = "true") boolean first,
    @Schema(description = "Whether this is the last page.", example = "false") boolean last,
    @Schema(description = "Whether this page has no content.", example = "false") boolean empty) {

  public static <T> PageResponse<T> from(Page<T> page) {
    return new PageResponse<>(
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
