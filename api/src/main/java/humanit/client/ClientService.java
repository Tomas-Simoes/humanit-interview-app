package humanit.client;

import humanit.client.dto.ClientResponse;
import humanit.client.dto.ClientSummaryResponse;
import humanit.client.dto.CreateClientRequest;
import humanit.client.dto.UpdateClientRequest;
import humanit.document.dto.CreateDocumentRequest;
import humanit.error.ApplicationException;
import humanit.error.ErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {
  private final ClientRepository clientRepository;
  private final ClientMapper clientMapper;

  public ClientService(ClientRepository clientRepository, ClientMapper clientMapper) {
    this.clientRepository = clientRepository;
    this.clientMapper = clientMapper;
  }

  @Transactional
  public ClientResponse createClient(CreateClientRequest request) {
    if (clientRepository.existsByEmail(request.email())) {
      throw new ApplicationException(
          ErrorCode.CLIENT_EMAIL_EXISTS, "A client with this email already exists.");
    }

    if (clientRepository.existsByTaxIdentifier(request.taxIdentifier())) {
      throw new ApplicationException(
          ErrorCode.CLIENT_TAX_IDENTIFIER_EXISTS,
          "A client with this tax identifier already exists.");
    }

    rejectDuplicateDocumentNumbers(request.documents());

    Client client = clientMapper.toEntity(request);
    Client savedClient = clientRepository.save(client);

    return clientMapper.toResponse(savedClient);
  }

  @Transactional(readOnly = true)
  public ClientResponse getClient(Long id) {
    Client client =
        clientRepository.findByIdWithDocuments(id).orElseThrow(() -> clientNotFoundException(id));

    return clientMapper.toResponse(client);
  }

  @Transactional
  public ClientResponse updateClient(Long id, UpdateClientRequest request) {
    Client client =
        clientRepository.findByIdWithDocuments(id).orElseThrow(() -> clientNotFoundException(id));

    if (clientRepository.existsByEmailAndIdNot(request.email(), id)) {
      throw new ApplicationException(
          ErrorCode.CLIENT_EMAIL_EXISTS, "A client with this email already exists.");
    }

    if (clientRepository.existsByTaxIdentifierAndIdNot(request.taxIdentifier(), id)) {
      throw new ApplicationException(
          ErrorCode.CLIENT_TAX_IDENTIFIER_EXISTS,
          "A client with this tax identifier already exists.");
    }

    clientMapper.updateEntity(client, request);

    return clientMapper.toResponse(client);
  }

  @Transactional
  public void deleteClient(Long id) {
    Client client = clientRepository.findById(id).orElseThrow(() -> clientNotFoundException(id));

    clientRepository.delete(client);
  }

  @Transactional(readOnly = true)
  public Page<ClientSummaryResponse> getClients(Pageable pageable) {
    return clientRepository.findClientSummaries(pageable);
  }

  @Transactional(readOnly = true)
  public Page<ClientResponse> getClientsWithDocuments(Pageable pageable) {
    Page<Client> clientPage = clientRepository.findAll(pageable);
    List<Long> clientIds = clientPage.getContent().stream().map(Client::getId).toList();

    if (clientIds.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, clientPage.getTotalElements());
    }

    Map<Long, Client> clientsWithDocuments =
        clientRepository.findAllByIdWithDocuments(clientIds).stream()
            .collect(Collectors.toMap(Client::getId, client -> client));

    List<ClientResponse> responses =
        clientIds.stream().map(clientsWithDocuments::get).map(clientMapper::toResponse).toList();

    return new PageImpl<>(responses, pageable, clientPage.getTotalElements());
  }

  private ApplicationException clientNotFoundException(Long id) {
    return new ApplicationException(
        ErrorCode.CLIENT_NOT_FOUND, "Client %d was not found.".formatted(id));
  }

  private void rejectDuplicateDocumentNumbers(List<CreateDocumentRequest> documents) {
    Set<String> numbers = new HashSet<>();
    for (CreateDocumentRequest document : documents) {
      if (!numbers.add(document.number())) {
        throw new ApplicationException(
            ErrorCode.DOCUMENT_NUMBER_EXISTS,
            "A document with number %s appears more than once in the create client request."
                .formatted(document.number()));
      }
    }
  }
}
