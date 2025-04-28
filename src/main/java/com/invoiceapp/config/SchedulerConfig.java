// src/main/java/com/invoiceapp/config/SchedulerConfig.java
package com.invoiceapp.config;

import com.invoiceapp.entity.Invoice;
import com.invoiceapp.entity.InvoiceMetric;
import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.entity.User;
import com.invoiceapp.repository.InvoiceMetricRepository;
import com.invoiceapp.repository.InvoiceRepository;
import com.invoiceapp.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulerConfig {

    private static final Logger log = LoggerFactory.getLogger(SchedulerConfig.class);

    private final InvoiceRepository       invoiceRepo;
    private final InvoiceMetricRepository metricRepo;
    private final EmailService            emailService;

    /**
     * Runs on the cron defined in application.properties.
     * (e.g. every minute in dev, once daily in prod).
     */
    @Scheduled(cron = "${invoiceapp.overdue.cron}")
    @Transactional
    public void processOverdueInvoices() {
        LocalDate today = LocalDate.now();

        List<Invoice> toProcess =
                invoiceRepo.findSentAndDueOnOrBefore(InvoiceStatus.SENT, today);

        if (toProcess.isEmpty()) {
            log.info("Overdue sweep: none to process at {}", today);
            return;
        }

        log.info("Overdue sweep: {} invoice(s) become OVERDUE", toProcess.size());
        for (Invoice inv : toProcess) {
            // 1) mark overdue + snapshot
            inv.setStatus(InvoiceStatus.OVERDUE);
            metricRepo.save(new InvoiceMetric(today, InvoiceStatus.OVERDUE, inv.getTotal()));

            // 2) email the client
            sendReminderToClient(inv);

            // 3) email the issuer
            sendReminderToIssuer(inv);
        }
    }

    private void sendReminderToClient(Invoice inv) {
        String subject = "Overdue Invoice " + inv.getInvoiceNumber();
        String body = String.format(
                "Dear %s,<br><br>Your invoice <strong>%s</strong> due %s for <strong>%s</strong> is now overdue."
                        + " Please pay as soon as possible.<br><br>Thank you.",
                inv.getClient().getName(),
                inv.getInvoiceNumber(),
                inv.getDueDate(),
                inv.getTotal()
        );
        emailService.sendHtml(inv.getClient().getEmail(), subject, body);
    }

    private void sendReminderToIssuer(Invoice inv) {
        User u = inv.getUser();
        String who = u.getEmail();
        String subject = "Client Overdue Invoice " + inv.getInvoiceNumber();
        String body = String.format(
                "Hello %s,<br><br>Your client <strong>%s</strong> has an overdue invoice <strong>%s</strong> "
                        + "due on %s for <strong>%s</strong>.<br><br>Regards,",
                who,
                inv.getClient().getName(),
                inv.getInvoiceNumber(),
                inv.getDueDate(),
                inv.getTotal()
        );
        emailService.sendHtml(u.getEmail(), subject, body);
    }
}
