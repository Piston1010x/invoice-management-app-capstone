package com.invoiceapp.util;

import com.invoiceapp.entity.User;
import com.invoiceapp.security.UserProvider;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvoiceNumberGenerator {

    private final EntityManager em;
    private final UserProvider userProvider;

    //Creates invoice numbers: “INV-00001”
    @Transactional
    public String next() {

        //get current user to determine which users count to use
        User user = userProvider.getCurrentUser();

        //grab the highest number that user already has
        Integer last = em.createQuery("""
             SELECT MAX(CAST(SUBSTRING(i.invoiceNumber, 5) AS int))
               FROM Invoice i
              WHERE i.user = :user
                AND i.invoiceNumber IS NOT NULL
             """, Integer.class)
                .setParameter("user", user)
                .getSingleResult();

        //if last invoice is null create first one, else increment last by 1
        int next = (last == null ? 1 : last + 1);

        //return new invoice number
        return "INV-%05d".formatted(next);
    }
}
