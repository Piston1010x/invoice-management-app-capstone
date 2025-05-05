package com.invoiceapp.config;

import com.invoiceapp.entity.Invoice;
import com.invoiceapp.entity.InvoiceMetric;
import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.entity.User;
import com.invoiceapp.repository.InvoiceMetricRepository;
import com.invoiceapp.repository.InvoiceRepository;
import com.invoiceapp.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulerConfig {


    private final InvoiceRepository invoiceRepository;
    private final InvoiceMetricRepository metricRepository;
    private final EmailService emailService;

    // cron for overdue invoices.
    @Scheduled(cron = "${invoiceapp.overdue.cron}")
    @Transactional
    public void processOverdueInvoices() {
        LocalDate today = LocalDate.now();
        List<Invoice> toProcess = invoiceRepository.findSentAndDueOnOrBefore(InvoiceStatus.SENT, today);

        if (toProcess.isEmpty()) {
            log.info("Overdue sweep: none to process at {}", today);
            return;
        }

        log.info("Overdue sweep: {} invoice(s) become OVERDUE", toProcess.size());
        for (Invoice inv : toProcess) {
            // 1) mark overdue + snapshot
            inv.setStatus(InvoiceStatus.OVERDUE);
            metricRepository.save(new InvoiceMetric(today, InvoiceStatus.OVERDUE, inv.getTotal()));

            // 2) email the client
            sendReminderToClient(inv);

            // 3) email the issuer
            sendReminderToIssuer(inv);
        }
    }


    //Send overdye reminder to client
    private void sendReminderToClient(Invoice invoice) {
        String subject = "Overdue Invoice " + invoice.getInvoiceNumber();
        String body = String.format(
                "Dear %s,<br><br>Your invoice <strong>%s</strong> due %s for <strong>%s</strong> is now overdue."
                        + " Please pay as soon as possible.<br><br>Thank you.",
                invoice.getClient().getName(),
                invoice.getInvoiceNumber(),
                invoice.getDueDate(),
                invoice.getTotal()
        );
        emailService.sendHtml(invoice.getClient().getEmail(), subject, body);
        log.info("Sent overdue reminder email to client {} for invoice {}", invoice.getClient().getEmail(), invoice.getInvoiceNumber());

    }

    //Send overdye reminder to the user
    private void sendReminderToIssuer(Invoice invoice) {
        User user = invoice.getUser();
        String who = user.getEmail();
        String subject = "Client Overdue Invoice " + invoice.getInvoiceNumber();
        String body = String.format(
                "Hello %s,<br><br>Your client <strong>%s</strong> has an overdue invoice <strong>%s</strong> "
                        + "due on %s for <strong>%s</strong>.<br><br>Regards,",
                who,
                invoice.getClient().getName(),
                invoice.getInvoiceNumber(),
                invoice.getDueDate(),
                invoice.getTotal()
        );
        emailService.sendHtml(user.getEmail(), subject, body);
        log.info("Sent overdue reminder email to issuer {} for invoice {}", user.getEmail(), invoice.getInvoiceNumber());

    }
}
