package com.invoiceapp.util;

import com.invoiceapp.dto.client.ClientRequest;
import com.invoiceapp.dto.client.ClientResponse;
import com.invoiceapp.entity.Client;
import org.springframework.stereotype.Component;

@Component
public final class ClientMapper {

    private ClientMapper() {}

    //Maps client request (dto) to client (entity)
    public static Client toEntity(ClientRequest dto) {
        Client c = new Client();
        c.setName(dto.name());
        c.setEmail(dto.email());
        c.setPhone(dto.phone());
        return c;
    }

    //Maps client request (entity) to client (dto)
    public static ClientResponse toDto(Client c) {
        return new ClientResponse(c.getId(), c.getName(), c.getEmail(), c.getPhone());
    }
}
