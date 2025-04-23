package com.invoiceapp.util;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvoiceNumberGenerator {

    private final EntityManager em;

    /** INV‑00001, INV‑00002 … */
    @Transactional
    public String next() {
        Integer last = em.createQuery("""
            SELECT MAX(CAST(SUBSTRING(i.invoiceNumber, 5) AS int))
            FROM Invoice i
            WHERE i.invoiceNumber IS NOT NULL
            """, Integer.class)
                .getSingleResult();

        int next = (last == null ? 1 : last + 1);
        return "INV-%05d".formatted(next);
    }
}
