package com.invoiceapp.service;

import com.invoiceapp.dto.*;
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
    private final ClientMapper mapper;          // <- mapper is a @Component bean
    private final UserProvider userProvider;
    private final InvoiceRepository invoiceRepository;
    public ClientResponse create(ClientRequest req) {
        Client entity = ClientMapper.toEntity(req);
        entity.setUser(userProvider.getCurrentUser()); // ✅ associate with user
        Client saved = repo.save(entity);
        return ClientMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public ClientResponse findById(Long id) {
        User user = userProvider.getCurrentUser();
        return repo.findById(id)
                .filter(client -> client.getUser().equals(user)) // ✅ enforce ownership
                .map(ClientMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Client %d not found or access denied".formatted(id)));
    }

    public ClientResponse update(Long id, ClientRequest req) {
        User user = userProvider.getCurrentUser();
        Client client = repo.findById(id)
                .filter(c -> c.getUser().equals(user)) // ✅ secure
                .orElseThrow(() -> new EntityNotFoundException("Client not found or access denied"));

        client.setName(req.name());
        client.setEmail(req.email());
        client.setPhone(req.phone());

        return ClientMapper.toDto(repo.save(client));
    }

    public void update(ClientForm form) {
        User user = userProvider.getCurrentUser();
        Client client = repo.findById(form.getId())
                .filter(c -> c.getUser().equals(user)) // ✅ secure
                .orElseThrow(() -> new EntityNotFoundException("Client not found or access denied"));

        client.setName(form.getName());
        client.setEmail(form.getEmail());
        client.setPhone(form.getPhone());
        repo.save(client); // <-- ADD THIS LINE TO PERSIST CHANGES
    }


    @Transactional
    public void delete(Long id) {

        User user = userProvider.getCurrentUser();
        Client client = repo.findById(id)
                .filter(c -> c.getUser().equals(user))
                .orElseThrow(() ->
                        new EntityNotFoundException("Client not found or access denied"));

        /* Block only invoices that still require action */
        var unpaidStatuses = List.of(
                InvoiceStatus.SENT,
                InvoiceStatus.OVERDUE);

        long unpaid = invoiceRepository
                .countByClientIdAndStatusInAndArchivedFalse(id, unpaidStatuses);

        if (unpaid > 0) {
            throw new ClientHasActiveInvoicesException(unpaid);   // ← still handled by advice
        }

        /* Physically delete every remaining invoice row (PAID, DRAFT, ARCHIVED) */
        invoiceRepository.deleteAllByClientId(id);

        repo.delete(client);
    }




    @Transactional(readOnly = true)
    public Page<ClientResponse> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        User user = userProvider.getCurrentUser();

        return repo.findAllByUser(user, pageable)
                .map(ClientMapper::toDto);
    }

    /* ───────── Simple list for dropdowns, etc. ───────── */
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
