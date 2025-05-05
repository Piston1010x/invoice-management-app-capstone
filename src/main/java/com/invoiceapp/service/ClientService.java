package com.invoiceapp.service;

import com.invoiceapp.dto.client.ClientForm;
import com.invoiceapp.dto.client.ClientRequest;
import com.invoiceapp.dto.client.ClientResponse;
import com.invoiceapp.entity.Client;
import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.entity.User;
import com.invoiceapp.exception.ClientHasActiveInvoicesException;
import com.invoiceapp.repository.ClientRepository;
import com.invoiceapp.repository.InvoiceRepository;
import com.invoiceapp.security.UserProvider;
import com.invoiceapp.util.ClientMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ClientService {


    //fields
    private final ClientRepository clientRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserProvider userProvider;


    //create method
    public ClientResponse create(ClientRequest req) {
        User user = userProvider.getCurrentUser();
        log.info("User {} is creating a client with name: {}", user.getEmail(), req.name());
        validateUnique(req, user, null);
        Client entity = ClientMapper.toEntity(req);
        entity.setUser(user);
        log.info("Client created with name: {}", req.name());
        return ClientMapper.toDto(clientRepository.save(entity));
    }



    @Transactional(readOnly = true)
    public ClientResponse findById(Long id) {
        User user = userProvider.getCurrentUser();
        log.info("User {} is finding client by ID: {}", user.getEmail(), id);
        return clientRepository.findById(id)
                .filter(c -> c.getUser().equals(user))
                .map(ClientMapper::toDto)
                .orElseThrow(() -> {
                    log.error("Client {} not found or access denied", id);
                    return new EntityNotFoundException("Client not found or access denied");
                });
    }

    public ClientResponse update(Long id, ClientRequest req) {
        User user = userProvider.getCurrentUser();
        log.info("User {} is updating client with ID: {}", user.getEmail(), id);
        Client client = clientRepository.findById(id)
                .filter(c -> c.getUser().equals(user))
                .orElseThrow(() -> {
                    log.error("Client {} not found or access denied", id);
                    return new EntityNotFoundException("Client not found or access denied");
                });
        validateUnique(req, user, id);
        client.setName(req.name());
        client.setEmail(req.email());
        client.setPhone(req.phone());
        log.info("Client with ID: {} updated successfully", id);
        return ClientMapper.toDto(clientRepository.save(client));
    }

    public void delete(Long id) {
        User user = userProvider.getCurrentUser();
        log.info("User {} is attempting to delete client with ID: {}", user.getEmail(), id);
        Client client = clientRepository.findById(id)
                .filter(c -> c.getUser().equals(user))
                .orElseThrow(() -> {
                    log.error("Client {} not found or access denied", id);
                    return new EntityNotFoundException("Client not found or access denied");
                });
        blockIfActiveInvoices(id);
        invoiceRepository.deleteAllByClientId(id);
        clientRepository.delete(client);
        log.info("Client with ID: {} deleted successfully", id);
    }

    @Transactional(readOnly = true)
    public Page<ClientResponse> list(int page, int size) {
        User user = userProvider.getCurrentUser();
        log.info("User {} is listing clients with pagination - Page: {}, Size: {}", user.getEmail(), page, size);
        Pageable pg = PageRequest.of(page, size, Sort.by("id").descending());
        return clientRepository.findAllByUser(user, pg)
                .map(ClientMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> findAll() {
        User user = userProvider.getCurrentUser();
        log.info("User {} is fetching all clients", user.getEmail());
        Pageable pg = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("name"));
        return clientRepository.findAllByUser(user, pg).stream()
                .map(ClientMapper::toDto).toList();
    }

    // ——— ADMIN “VIEW-AS” OPERATIONS ———

    @Transactional(readOnly = true)
    public List<ClientResponse> findAllForUser(User user) {
        log.info("Fetching all clients for admin user {}", user.getEmail());
        Pageable pg = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("name"));
        return clientRepository.findAllByUser(user, pg).stream()
                .map(ClientMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Page<ClientResponse> listForUser(User user, int page, int size) {
        log.info("Admin user {} is listing clients for another user {} with pagination - Page: {}, Size: {}",
                user.getEmail(), user.getEmail(), page, size);
        Pageable pg = PageRequest.of(page, size, Sort.by("id").descending());
        return clientRepository.findAllByUser(user, pg).map(ClientMapper::toDto);
    }

    @Transactional(readOnly = true)
    public ClientResponse findByIdForUser(Long id, User user) {
        log.info("Fetching client with ID {} for admin user {}", id, user.getEmail());
        return clientRepository.findById(id)
                .filter(c -> c.getUser().equals(user))
                .map(ClientMapper::toDto)
                .orElseThrow(() -> {
                    log.error("Client {} not found or access denied", id);
                    return new EntityNotFoundException("Client not found or access denied");
                });
    }

    public ClientResponse createForUser(ClientForm form, User user) {
        log.info("Admin user {} is creating a client for user {}", user.getEmail(), user.getEmail());

        // Trim and normalize the input data
        String name = normalize(form.getName().trim());
        String email = normalize(form.getEmail().trim());
        String phone = normalize(form.getPhone().trim());

        // Create the client request
        ClientRequest req = new ClientRequest(name, email, phone);

        // Validate uniqueness before proceeding
        validateUnique(req, user, null);  // Check for duplicates

        // Map to entity and set user
        Client entity = ClientMapper.toEntity(req);
        entity.setUser(user);

        log.info("Client for user {} created with name: {}", user.getEmail(), req.name());
        return ClientMapper.toDto(clientRepository.save(entity));  // Save the client after validation
    }

    public ClientResponse updateForUser(ClientForm form, User user) {
        log.info("Admin user {} is updating client with ID: {} for user {}", user.getEmail(), form.getId(), user.getEmail());

        // Trim and normalize the input data
        String name = normalize(form.getName().trim());
        String email = normalize(form.getEmail().trim());
        String phone = normalize(form.getPhone().trim());

        // Create the client request
        ClientRequest req = new ClientRequest(name, email, phone);

        // Validate uniqueness before proceeding
        validateUnique(req, user, form.getId());  // Check for duplicates excluding the current client ID

        // Find the client by ID and ensure the user has access
        Client client = clientRepository.findById(form.getId())
                .filter(c -> c.getUser().equals(user))
                .orElseThrow(() -> new EntityNotFoundException("Client not found or access denied"));

        // Update the client with the new data
        client.setName(req.name());
        client.setEmail(req.email());
        client.setPhone(req.phone());

        log.info("Client with ID: {} updated for user {}", form.getId(), user.getEmail());
        return ClientMapper.toDto(clientRepository.save(client));  // Save the updated client
    }

    private String normalize(String input) {
        // Normalize and remove characters, if any
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "");  // Remove non-ASCII characters like accents
    }



    //new delete method
    public void deleteForUser(Long id, User user) {
        log.info("Admin user {} is attempting to delete client with ID: {} for user {}", user.getEmail(), id, user.getEmail());
        Client client = clientRepository.findById(id)
                .filter(c -> c.getUser().equals(user))
                .orElseThrow(() -> new EntityNotFoundException("Client not found or access denied"));
        blockIfActiveInvoices(id);
        invoiceRepository.deleteAllByClientId(id);
        clientRepository.delete(client);
        log.info("Client with ID: {} deleted for user {}", id, user.getEmail());
    }

    //helper methods

    //method to validate client uniqueness
    private void validateUnique(ClientRequest req, User user, Long excludingId) {
        log.info("Validating uniqueness for client: {}", req.name());

        // Determine if it's a create or update operation
        boolean isUpdate = excludingId != null;

        //name check
        boolean nameExists;
        if (isUpdate) {
            nameExists = clientRepository.existsByNameAndUserAndIdNot(req.name(), user, excludingId);
        } else {
            nameExists = clientRepository.existsByNameAndUser(req.name(), user); // Use the create method
        }
        log.info("Does name exist: {}", nameExists);
        if (nameExists) {
            log.error("Client with name {} already exists for user {}", req.name(), user.getEmail());
            throw new IllegalArgumentException("Another client with this name already exists.");
        }

        //email check
        boolean emailExists;
        if (isUpdate) {
            emailExists = clientRepository.existsByEmailAndUserAndIdNot(req.email(), user, excludingId);
        } else {
            emailExists = clientRepository.existsByEmailAndUser(req.email(), user); // Use the create method
        }
        log.info("Does email exist: {}", emailExists);
        if (emailExists) {
            log.error("Client with email {} already exists for user {}", req.email(), user.getEmail());
            throw new IllegalArgumentException("Another client with this email already exists.");
        }

        //Phone check
        boolean phoneExists;
        if (isUpdate) {
            phoneExists = clientRepository.existsByPhoneAndUserAndIdNot(req.phone(), user, excludingId);
        } else {
            phoneExists = clientRepository.existsByPhoneAndUser(req.phone(), user); // Use the create method
        }
        log.info("Does phone exist: {}", phoneExists);
        if (phoneExists) {
            log.error("Client with phone {} already exists for user {}", req.phone(), user.getEmail());
            throw new IllegalArgumentException("Another client with this phone already exists.");
        }
    }

    //method to detect active invoices
    private void blockIfActiveInvoices(Long clientId) {
        log.info("Checking for active invoices for client ID: {}", clientId);
        var unpaid = List.of(InvoiceStatus.SENT, InvoiceStatus.OVERDUE);
        long cnt = invoiceRepository.countByClientIdAndStatusInAndArchivedFalse(clientId, unpaid);
        if (cnt > 0) {
            log.error("Client {} has active invoices. Cannot delete.", clientId);
            throw new ClientHasActiveInvoicesException(cnt);
        }
    }
}
