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

    /** Creates “INV-00001”, “INV-00002”, … **for the current user**. */
    @Transactional
    public String next() {

        User user = userProvider.getCurrentUser();   // ① whose counter?

        // ② grab the highest number that user already has
        Integer last = em.createQuery("""
             SELECT MAX(CAST(SUBSTRING(i.invoiceNumber, 5) AS int))
               FROM Invoice i
              WHERE i.user = :user
                AND i.invoiceNumber IS NOT NULL
             """, Integer.class)
                .setParameter("user", user)
                .getSingleResult();

        int next = (last == null ? 1 : last + 1);

        return "INV-%05d".formatted(next);
    }
}
