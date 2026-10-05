package humanit.client;

import java.net.URI;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import humanit.client.dto.ClientResponse;
import humanit.client.dto.ClientSummaryResponse;
import humanit.client.dto.CreateClientRequest;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {
    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

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

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClient(@PathVariable Long id) {
        ClientResponse clientResponse = clientService.getClient(id);
        return ResponseEntity.ok(clientResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }

    // TODO add config to cap page size
    // TODO create a pagination response DTO
    @GetMapping
    public ResponseEntity<Page<ClientSummaryResponse>> getClients(
            @PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
        return ResponseEntity.ok(clientService.getClients(pageable));
    }
}
