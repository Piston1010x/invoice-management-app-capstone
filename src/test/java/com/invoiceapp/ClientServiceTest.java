
package com.invoiceapp;

import com.invoiceapp.dto.client.ClientForm;
import com.invoiceapp.dto.client.ClientRequest;
import com.invoiceapp.dto.client.ClientResponse;
import com.invoiceapp.entity.Client;
import com.invoiceapp.entity.User;
import com.invoiceapp.repository.ClientRepository;
import com.invoiceapp.security.UserProvider;
import com.invoiceapp.service.ClientService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClientServiceTest {

    @Mock private ClientRepository clientRepository;
    @Mock private UserProvider userProvider;
    // No mock for ClientMapper if using static methods

    @InjectMocks
    private ClientService clientService;

    private User testUser;
    private Client testClient;
    private ClientRequest createRequest;
    private ClientForm updateForm;


    @BeforeEach
    void setUp() {
        testUser = new User(1L, "user@test.com", "pass", com.invoiceapp.entity.Role.USER, true);
        testClient = new Client(1L, "Existing Client", "exist@test.com", "111", testUser);
        createRequest = new ClientRequest("New Client", "new@test.com", "222");
        updateForm = new ClientForm();
        updateForm.setId(1L);
        updateForm.setName("Updated Client");
        updateForm.setEmail("updated@test.com");
        updateForm.setPhone("333");


        // Common stubbing
        lenient().when(userProvider.getCurrentUser()).thenReturn(testUser);
        lenient().when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
        lenient().when(clientRepository.findById(999L)).thenReturn(Optional.empty()); // Not found case
    }

    // --- CREATE Tests ---
    @Test
    void create_ValidRequest_ShouldSetUserAndSave() {
        // Arrange
        ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
        // Mock save to return the saved entity with ID
        when(clientRepository.save(clientCaptor.capture())).thenAnswer(invocation -> {
            Client saved = invocation.getArgument(0);
            saved.setId(2L); // Simulate ID generation
            return saved;
        });

        // Act
        ClientResponse response = clientService.create(createRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(2L); // Check ID from mocked save
        assertThat(response.name()).isEqualTo(createRequest.name());

        Client savedClient = clientCaptor.getValue();
        assertThat(savedClient.getUser()).isEqualTo(testUser); // Verify user was set
        assertThat(savedClient.getName()).isEqualTo(createRequest.name());
        assertThat(savedClient.getEmail()).isEqualTo(createRequest.email());
        assertThat(savedClient.getPhone()).isEqualTo(createRequest.phone());

        verify(clientRepository).save(any(Client.class));
    }

    // --- FIND BY ID Tests ---
    @Test
    void findById_ExistingClientOwnedByUser_ShouldReturnDto() {
        ClientResponse response = clientService.findById(1L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo(testClient.getName());
    }

    @Test
    void findById_ExistingClientNotOwnedByUser_ShouldThrowException() {
        // Arrange: Create a client owned by a *different* user
        User otherUser = new User(2L, "other@test.com", "pass", com.invoiceapp.entity.Role.USER, true);
        Client otherClient = new Client(2L, "Other's Client", "other@client.com", "555", otherUser);
        when(clientRepository.findById(2L)).thenReturn(Optional.of(otherClient));
        // Current user is still testUser (ID 1L) from setup

        // Act & Assert
        assertThatThrownBy(() -> clientService.findById(2L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found or access denied"); // Check specific message if possible
    }

    @Test
    void findById_NonExistentClient_ShouldThrowException() {
        assertThatThrownBy(() -> clientService.findById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found or access denied");
    }


    // --- UPDATE (ClientForm) Tests ---
    @Test
    void update_WithClientForm_OwnedClient_ShouldUpdateFields() {
        // Arrange
        ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
        when(clientRepository.save(clientCaptor.capture())).thenReturn(testClient); // Mock save if needed

        // Act
        clientService.update(updateForm); // Using the void update(ClientForm) method

        // Assert
        Client updatedClient = clientCaptor.getValue();
        assertThat(updatedClient.getId()).isEqualTo(1L); // Ensure it's the same client
        assertThat(updatedClient.getName()).isEqualTo(updateForm.getName());
        assertThat(updatedClient.getEmail()).isEqualTo(updateForm.getEmail());
        assertThat(updatedClient.getPhone()).isEqualTo(updateForm.getPhone());
        assertThat(updatedClient.getUser()).isEqualTo(testUser); // Ensure user wasn't changed

        verify(clientRepository).save(any(Client.class)); // Verify save was called
    }

    @Test
    void update_WithClientForm_ClientNotFound_ShouldThrowException() {
        updateForm.setId(999L); // Set form ID to non-existent client
        assertThatThrownBy(() -> clientService.update(updateForm))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found or access denied");
        verify(clientRepository, never()).save(any());
    }

    @Test
    void update_WithClientForm_ClientNotOwned_ShouldThrowException() {
        // Arrange: Client exists but owned by someone else
        User otherUser = new User(2L, "other@test.com", "pass", com.invoiceapp.entity.Role.USER, true);
        Client otherClient = new Client(2L, "Other's Client", "other@client.com", "555", otherUser);
        when(clientRepository.findById(2L)).thenReturn(Optional.of(otherClient));

        updateForm.setId(2L); // Try to update the other user's client

        // Act & Assert
        assertThatThrownBy(() -> clientService.update(updateForm))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found or access denied");
        verify(clientRepository, never()).save(any());
    }


    @Test
    void delete_ClientNotFound_ShouldThrowException() {
        assertThatThrownBy(() -> clientService.delete(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found or access denied");
        verify(clientRepository, never()).delete(any());
    }

    @Test
    void delete_ClientNotOwned_ShouldThrowException() {
        // Arrange: Client exists but owned by someone else
        User otherUser = new User(2L, "other@test.com", "pass", com.invoiceapp.entity.Role.USER, true);
        Client otherClient = new Client(2L, "Other's Client", "other@client.com", "555", otherUser);
        when(clientRepository.findById(2L)).thenReturn(Optional.of(otherClient));

        // Act & Assert
        assertThatThrownBy(() -> clientService.delete(2L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found or access denied");
        verify(clientRepository, never()).delete(any());
    }


    // --- LIST / FINDALL Tests ---
    @Test
    void list_ShouldCallRepositoryAndMapResults() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").descending());
        Page<Client> mockPage = new PageImpl<>(List.of(testClient), pageable, 1);
        given(clientRepository.findAllByUser(testUser, pageable)).willReturn(mockPage);

        Page<ClientResponse> result = clientService.list(0, 10);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(testClient.getId());
        verify(clientRepository).findAllByUser(testUser, pageable);
    }

    @Test
    void findAll_ShouldCallRepositoryAndMapResults() {
        // Arrange
        Pageable expectedPageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("name"));
        Page<Client> mockPage = new PageImpl<>(List.of(testClient)); // Simulate finding one client
        given(clientRepository.findAllByUser(testUser, expectedPageable)).willReturn(mockPage);


        // Act
        List<ClientResponse> result = clientService.findAll();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(testClient.getId());
        verify(clientRepository).findAllByUser(testUser, expectedPageable);

    }
}
