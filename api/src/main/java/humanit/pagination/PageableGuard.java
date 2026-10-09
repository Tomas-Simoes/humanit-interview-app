package humanit.pagination;

import humanit.error.ApplicationException;
import humanit.error.ErrorCode;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class PageableGuard {
  public void requireAllowedSort(Pageable pageable, Set<String> allowedFields) {
    var invalidFields =
        pageable.getSort().stream()
            .map(order -> order.getProperty())
            .filter(field -> !allowedFields.contains(field))
            .distinct()
            .toList();

    if (!invalidFields.isEmpty()) {
      throw new ApplicationException(
          ErrorCode.INVALID_SORT,
          "Unsupported sort field(s): %s.".formatted(String.join(", ", invalidFields)));
    }
  }
}
