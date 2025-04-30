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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientService {

    private final ClientRepository repo;
    private final ClientMapper mapper;
    private final UserProvider userProvider;
    private final InvoiceRepository invoiceRepository;

    //Create new client
    public ClientResponse create(ClientRequest req) {

        User user = userProvider.getCurrentUser();

        if (repo.existsByNameAndUser(req.name(), user)) {
            throw new IllegalArgumentException("A client with this name already exists.");
        }

        if (repo.existsByEmailAndUser(req.email(), user)) {
            throw new IllegalArgumentException("A client with this email already exists.");
        }
        if (repo.existsByPhoneAndUser(req.phone(), user)) {
            throw new IllegalArgumentException("A client with this phone already exists.");
        }

        Client entity = ClientMapper.toEntity(req);
        entity.setUser(user);
        Client saved = repo.save(entity);
        return ClientMapper.toDto(saved);
    }

    //find client by id
    @Transactional(readOnly = true)
    public ClientResponse findById(Long id) {
        User user = userProvider.getCurrentUser();
        return repo.findById(id)
                .filter(client -> client.getUser().equals(user))
                .map(ClientMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Client %d not found or access denied".formatted(id)));
    }

    //update for rest api
    public ClientResponse update(Long id, ClientRequest req) {
        User user = userProvider.getCurrentUser();
        Client client = repo.findById(id)
                .filter(c -> c.getUser().equals(user))
                .orElseThrow(() -> new EntityNotFoundException("Client not found or access denied"));

        if (repo.existsByNameAndUserAndIdNot(req.name(), user, id)) {
            throw new IllegalArgumentException("Another client with this name already exists.");
        }

        // Check for conflicts (exclude current client)
        if (repo.existsByEmailAndUserAndIdNot(req.email(), user, id)) {
            throw new IllegalArgumentException("Another client with this email already exists.");
        }
        if (repo.existsByPhoneAndUserAndIdNot(req.phone(), user, id)) {
            throw new IllegalArgumentException("Another client with this phone already exists.");
        }

        client.setName(req.name());
        client.setEmail(req.email());
        client.setPhone(req.phone());

        return ClientMapper.toDto(repo.save(client));
    }

    //update client info for ui layer
    public void update(ClientForm form) {
        User user = userProvider.getCurrentUser();
        Client client = repo.findById(form.getId())
                .filter(c -> c.getUser().equals(user))
                .orElseThrow(() -> new EntityNotFoundException("Client not found or access denied"));

        if (repo.existsByNameAndUserAndIdNot(form.getName(), user, form.getId())) {
            throw new IllegalArgumentException("Another client with this name already exists.");
        }

        if (repo.existsByEmailAndUserAndIdNot(form.getEmail(), user, form.getId())) {
            throw new IllegalArgumentException("Another client with this email already exists.");
        }
        if (repo.existsByPhoneAndUserAndIdNot(form.getPhone(), user, form.getId())) {
            throw new IllegalArgumentException("Another client with this phone already exists.");
        }

        client.setName(form.getName());
        client.setEmail(form.getEmail());
        client.setPhone(form.getPhone());
        repo.save(client);
    }




    //delete client (deletion is not allowed while there are active invoices)
    @Transactional
    public void delete(Long id) {

        User user = userProvider.getCurrentUser();
        Client client = repo.findById(id)
                .filter(c -> c.getUser().equals(user))
                .orElseThrow(() ->
                        new EntityNotFoundException("Client not found or access denied"));

        // Block only invoices that still require action
        var unpaidStatuses = List.of(
                InvoiceStatus.SENT,
                InvoiceStatus.OVERDUE);

        long unpaid = invoiceRepository
                .countByClientIdAndStatusInAndArchivedFalse(id, unpaidStatuses);

        if (unpaid > 0) {
            throw new ClientHasActiveInvoicesException(unpaid);
        }

        // Physically delete every remaining invoice row (PAID, DRAFT, ARCHIVED)
        invoiceRepository.deleteAllByClientId(id);

        repo.delete(client);
    }



    //paged client list
    @Transactional(readOnly = true)
    public Page<ClientResponse> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        User user = userProvider.getCurrentUser();

        return repo.findAllByUser(user, pageable)
                .map(ClientMapper::toDto);
    }

    //full client list
    @Transactional(readOnly = true)
    public List<ClientResponse> findAll() {
        User user = userProvider.getCurrentUser();
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("name"));
        return repo.findAllByUser(user, pageable)
                .stream()
                .map(ClientMapper::toDto)
                .toList();

    }
}
