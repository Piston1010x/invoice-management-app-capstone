package com.invoiceapp;

import com.invoiceapp.dto.client.ClientForm;
import com.invoiceapp.dto.client.ClientRequest;
import com.invoiceapp.dto.client.ClientResponse;
import com.invoiceapp.entity.Client;
import com.invoiceapp.entity.User;
import com.invoiceapp.exception.ClientHasActiveInvoicesException;
import com.invoiceapp.repository.ClientRepository;
import com.invoiceapp.repository.InvoiceRepository;
import com.invoiceapp.security.UserProvider;
import com.invoiceapp.service.ClientService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class ClientServiceTest {

    private ClientRepository clientRepo;
    private InvoiceRepository invoiceRepo;
    private UserProvider userProvider;
    private ClientService service;

    private User user;
    private Client existingClient;

    @BeforeEach
    void setUp() {
        clientRepo = mock(ClientRepository.class);
        invoiceRepo = mock(InvoiceRepository.class);
        userProvider = mock(UserProvider.class);
        service = new ClientService(clientRepo, invoiceRepo, userProvider);

        user = new User();
        user.setId(42L);
        user.setEmail("test@example.com");
        when(userProvider.getCurrentUser()).thenReturn(user);

        existingClient = new Client();
        existingClient.setId(100L);
        existingClient.setName("Acme Corp");
        existingClient.setEmail("contact@acme.com");
        existingClient.setPhone("12345");
        existingClient.setUser(user);
    }

    //create test
    @Test
    void create_happyPath() {
        ClientRequest req = new ClientRequest("NewCo", "hello@new.co", "555");
        when(clientRepo.existsByNameAndUser(req.name(), user)).thenReturn(false);
        when(clientRepo.existsByEmailAndUser(req.email(), user)).thenReturn(false);
        when(clientRepo.existsByPhoneAndUser(req.phone(), user)).thenReturn(false);

        Client saved = new Client();
        saved.setId(1L);
        saved.setName(req.name());
        saved.setEmail(req.email());
        saved.setPhone(req.phone());
        saved.setUser(user);
        when(clientRepo.save(any())).thenReturn(saved);

        ClientResponse resp = service.create(req);

        assertEquals(1L, resp.id());
        assertEquals("NewCo", resp.name());
        assertEquals("hello@new.co", resp.email());
        assertEquals("555", resp.phone());
    }


    //duplicate
    @Test
    void create_duplicateFields_throw() {
        ClientRequest req = new ClientRequest("DupCo", "dup@co.com", "777");
        when(clientRepo.existsByNameAndUser(req.name(), user)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.create(req));

        reset(clientRepo);
        when(clientRepo.existsByNameAndUser(req.name(), user)).thenReturn(false);
        when(clientRepo.existsByEmailAndUser(req.email(), user)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.create(req));

        reset(clientRepo);
        when(clientRepo.existsByNameAndUser(req.name(), user)).thenReturn(false);
        when(clientRepo.existsByEmailAndUser(req.email(), user)).thenReturn(false);
        when(clientRepo.existsByPhoneAndUser(req.phone(), user)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.create(req));
    }

    //findById tests
    @Test
    void findById_found() {
        when(clientRepo.findById(100L)).thenReturn(Optional.of(existingClient));
        ClientResponse resp = service.findById(100L);
        assertEquals(100L, resp.id());
    }

    @Test
    void findById_notFound_throws() {
        when(clientRepo.findById(999L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.findById(999L));
    }

    //update tests
    @Test
    void update_happyPath() {
        ClientRequest req = new ClientRequest("AcmeX", "new@acme.com", "999");
        when(clientRepo.findById(100L)).thenReturn(Optional.of(existingClient));
        when(clientRepo.existsByNameAndUserAndIdNot(req.name(), user, 100L)).thenReturn(false);
        when(clientRepo.existsByEmailAndUserAndIdNot(req.email(), user, 100L)).thenReturn(false);
        when(clientRepo.existsByPhoneAndUserAndIdNot(req.phone(), user, 100L)).thenReturn(false);
        when(clientRepo.save(existingClient)).thenReturn(existingClient);

        ClientResponse resp = service.update(100L, req);
        assertEquals("AcmeX", resp.name());
    }

    @Test
    void update_duplicateFields_throw() {
        ClientRequest req = new ClientRequest("Unique", "dup@acme.com", "000");
        when(clientRepo.findById(100L)).thenReturn(Optional.of(existingClient));
        when(clientRepo.existsByNameAndUserAndIdNot(req.name(), user, 100L)).thenReturn(false);
        when(clientRepo.existsByEmailAndUserAndIdNot(req.email(), user, 100L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.update(100L, req));
    }

    @Test
    void update_notFound_throws() {
        when(clientRepo.findById(50L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> service.update(50L, new ClientRequest("X", "x@x.com", "1")));
    }

    // — delete() tests —

    @Test
    void delete_noActiveInvoices_deletes() {
        when(clientRepo.findById(100L)).thenReturn(Optional.of(existingClient));
        when(invoiceRepo.countByClientIdAndStatusInAndArchivedFalse(eq(100L), anyList()))
                .thenReturn(0L);

        service.delete(100L);

        verify(invoiceRepo).deleteAllByClientId(100L);
        verify(clientRepo).delete(existingClient);
    }

    @Test
    void delete_withActiveInvoices_throws() {
        when(clientRepo.findById(100L)).thenReturn(Optional.of(existingClient));
        when(invoiceRepo.countByClientIdAndStatusInAndArchivedFalse(eq(100L), anyList()))
                .thenReturn(3L);

        assertThrows(ClientHasActiveInvoicesException.class, () -> service.delete(100L));
        verify(clientRepo, never()).delete(any());
    }

    // — pagination/listing tests —

    @Test
    void list_returnsPagedClients() {
        Pageable pg = PageRequest.of(1, 5, Sort.by("id").descending());
        List<Client> clients = List.of(existingClient);
        // total elements = 10 → totalPages = ceil(10/5) = 2
        when(clientRepo.findAllByUser(user, pg))
                .thenReturn(new PageImpl<>(clients, pg, 10));

        Page<ClientResponse> page = service.list(1, 5);
        assertEquals(10, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
    }

    @Test
    void findAll_returnsAllClientsSorted() {
        Pageable pg = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("name"));
        when(clientRepo.findAllByUser(user, pg))
                .thenReturn(new PageImpl<>(List.of(existingClient), pg, 1));

        List<ClientResponse> list = service.findAll();
        assertEquals(1, list.size());
    }

    // — admin “forUser” tests —

    @Test
    void updateForUser_happyPath() {
        User other = new User();
        other.setEmail("admin@foo");
        // make sure the fetched client has the correct user
        existingClient.setUser(other);
        when(clientRepo.findById(100L)).thenReturn(Optional.of(existingClient));

        ClientForm form = new ClientForm();
        form.setId(100L);
        form.setName("NewName");
        form.setEmail("new@foo.com");
        form.setRawPhone("123");
        form.setPhone("123");

        when(clientRepo.existsByNameAndUserAndIdNot("NewName", other, 100L)).thenReturn(false);
        when(clientRepo.existsByEmailAndUserAndIdNot("new@foo.com", other, 100L)).thenReturn(false);
        when(clientRepo.existsByPhoneAndUserAndIdNot("123", other, 100L)).thenReturn(false);
        when(clientRepo.save(existingClient)).thenReturn(existingClient);

        ClientResponse resp = service.updateForUser(form, other);
        assertEquals("NewName", resp.name());
    }

    @Test
    void deleteForUser_withActiveInvoices_throws() {
        User other = new User();
        other.setEmail("admin@foo");
        existingClient.setUser(other);  // ensure filter passes
        when(clientRepo.findById(100L)).thenReturn(Optional.of(existingClient));
        when(invoiceRepo.countByClientIdAndStatusInAndArchivedFalse(eq(100L), anyList()))
                .thenReturn(5L);

        assertThrows(ClientHasActiveInvoicesException.class,
                () -> service.deleteForUser(100L, other));
        verify(clientRepo, never()).delete(any());
    }

    @Test
    void deleteForUser_noActiveInvoices_deletes() {
        User other = new User();
        other.setEmail("admin@foo");
        existingClient.setUser(other);
        when(clientRepo.findById(100L)).thenReturn(Optional.of(existingClient));
        when(invoiceRepo.countByClientIdAndStatusInAndArchivedFalse(eq(100L), anyList()))
                .thenReturn(0L);

        service.deleteForUser(100L, other);
        verify(invoiceRepo).deleteAllByClientId(100L);
        verify(clientRepo).delete(existingClient);
    }
}
