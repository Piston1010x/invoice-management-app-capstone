package com.invoiceapp.service;

import com.invoiceapp.dto.*;
import com.invoiceapp.entity.Client;
import com.invoiceapp.repository.ClientRepository;
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


    public ClientResponse create(ClientRequest req) {
        Client saved = repo.save(ClientMapper.toEntity(req));
        return ClientMapper.toDto(saved);
    }



    @Transactional(readOnly = true)
    public ClientResponse findById(Long id) {
        return repo.findById(id)
                .map(ClientMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Client %d not found".formatted(id)));
    }

    public ClientResponse update(Long id, ClientRequest req) {
        Client client = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client not found"));

        client.setName(req.name());
        client.setEmail(req.email());
        client.setPhone(req.phone());

        return ClientMapper.toDto(repo.save(client));
    }


    public void delete(Long id) {
        repo.deleteById(id);
    }

    public void update(ClientForm form) {
        Client client = repo.findById(form.getId())
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + form.getId()));

        client.setName(form.getName());
        client.setEmail(form.getEmail());
        client.setPhone(form.getPhone());
    }
    @Transactional(readOnly = true)
    public Page<ClientResponse> list(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        return repo.findAll(pageable)
                .map(ClientMapper::toDto);         // Page.map() keeps paging metadata
    }

    /* ───────── Simple list for dropdowns, etc. ───────── */
    @Transactional(readOnly = true)
    public List<ClientResponse> findAll() {

        return repo.findAll(Sort.by("name"))
                .stream()
                .map(ClientMapper::toDto)
                .toList();
    }
}
