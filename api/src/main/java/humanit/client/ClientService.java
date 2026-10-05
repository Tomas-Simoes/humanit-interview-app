package humanit.client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import humanit.client.dto.ClientResponse;
import humanit.client.dto.ClientSummaryResponse;
import humanit.client.dto.CreateClientRequest;

/*
TODO
    * define explicit throw errors
*/

@Service
public class ClientService {
    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    public ClientService(ClientRepository clientRepository, ClientMapper clientMapper) {
        this.clientRepository = clientRepository;
        this.clientMapper = clientMapper;
    }

    // TODO catch DataIntegrityViolationException on GlobalExceptionHandler
    @Transactional
    public ClientResponse createClient(CreateClientRequest request) {
        if (clientRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists.");
        }

        if (clientRepository.existsByTaxIdentifier(request.taxIdentifier())) {
            throw new IllegalArgumentException("Tax identifier already exists");
        }

        Client client = clientMapper.toEntity(request);
        Client savedClient = clientRepository.save(client);

        return clientMapper.toResponse(savedClient);
    }

    @Transactional(readOnly = true)
    public ClientResponse getClient(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Client not found"));

        return clientMapper.toResponse(client);
    }

    @Transactional
    public void deleteClient(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Client not found"));

        clientRepository.delete(client);
    }

    @Transactional(readOnly = true)
    public Page<ClientSummaryResponse> getClients(Pageable pageable) {
        return clientRepository.findClientSummaries(pageable);
    }

}
