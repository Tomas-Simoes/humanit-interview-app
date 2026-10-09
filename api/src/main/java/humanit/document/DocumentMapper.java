package humanit.document;

import humanit.document.dto.CreateDocumentRequest;
import humanit.document.dto.DocumentResponse;
import humanit.document.dto.UpdateDocumentRequest;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {
  public Document toEntity(CreateDocumentRequest request) {
    return new Document(request.number(), request.description(), request.expirationDate());
  }

  public DocumentResponse toResponse(Document document) {
    return new DocumentResponse(
        document.getId(),
        document.getNumber(),
        document.getDescription(),
        document.getExpirationDate(),
        document.getClient().getId());
  }

  public void updateEntity(Document document, UpdateDocumentRequest request) {
    document.updateDetails(request.number(), request.description(), request.expirationDate());
  }
}
