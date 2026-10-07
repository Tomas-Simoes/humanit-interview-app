package humanit.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import humanit.client.Client;
import humanit.client.ClientRepository;
import humanit.document.dto.CreateDocumentRequest;
import humanit.document.dto.DocumentResponse;
import humanit.document.dto.DocumentSummaryResponse;
import humanit.document.dto.UpdateDocumentRequest;
import humanit.error.ApplicationException;
import humanit.error.ErrorCode;

@Service
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final ClientRepository clientRepository;
    private final DocumentMapper documentMapper;

    public DocumentService(
            DocumentRepository documentRepository,
            ClientRepository clientRepository,
            DocumentMapper documentMapper) {
        this.documentRepository = documentRepository;
        this.clientRepository = clientRepository;
        this.documentMapper = documentMapper;
    }

    @Transactional
    public DocumentResponse createDocument(Long clientId, CreateDocumentRequest request) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> clientNotFound(clientId));

        if (documentRepository.existsByClientIdAndNumber(clientId, request.number())) {
            throw new ApplicationException(
                    ErrorCode.DOCUMENT_NUMBER_EXISTS,
                    "A document with this number already exists for client %d.".formatted(clientId));
        }

        Document document = documentMapper.toEntity(request);
        client.addDocument(document);
        Document savedDocument = documentRepository.save(document);

        return documentMapper.toResponse(savedDocument);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(Long clientId, Long id) {
        ensureClientExists(clientId);

        Document document = documentRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> documentNotFound(clientId, id));

        return documentMapper.toResponse(document);
    }

    @Transactional
    public DocumentResponse updateDocument(Long clientId, Long id, UpdateDocumentRequest request) {
        ensureClientExists(clientId);

        Document document = documentRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> documentNotFound(clientId, id));

        if (documentRepository.existsByClientIdAndNumberAndIdNot(clientId, request.number(), id)) {
            throw new ApplicationException(
                    ErrorCode.DOCUMENT_NUMBER_EXISTS,
                    "A document with this number already exists for client %d.".formatted(clientId));
        }

        documentMapper.updateEntity(document, request);

        return documentMapper.toResponse(document);
    }

    @Transactional
    public void deleteDocument(Long clientId, Long id) {
        ensureClientExists(clientId);

        Document document = documentRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> documentNotFound(clientId, id));

        documentRepository.delete(document);
    }

    @Transactional(readOnly = true)
    public Page<DocumentSummaryResponse> getDocuments(Long clientId, Pageable pageable) {
        ensureClientExists(clientId);

        return documentRepository.findDocumentSummariesByClientId(clientId, pageable);
    }

    private void ensureClientExists(Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw clientNotFound(clientId);
        }
    }

    private ApplicationException clientNotFound(Long clientId) {
        return new ApplicationException(
                ErrorCode.CLIENT_NOT_FOUND,
                "Client %d was not found.".formatted(clientId));
    }

    private ApplicationException documentNotFound(Long clientId, Long id) {
        return new ApplicationException(
                ErrorCode.DOCUMENT_NOT_FOUND,
                "Document %d was not found for client %d.".formatted(id, clientId));
    }
}
