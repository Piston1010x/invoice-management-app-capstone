//src/main/java/com/invoiceapp/config/SchedulerConfig.java
package com.invoiceapp.config;

import com.invoiceapp.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@EnableScheduling
@Component
@RequiredArgsConstructor
@Configuration
public class SchedulerConfig {

    private final InvoiceService invoices;

    @Scheduled(cron = "0 30 0 * * *")   // 00:30 every day
    public void nightlyOverdueSweep() {
        int n = invoices.markOverdue();
        if (n > 0)                       // optional log
            System.out.println("Over-due sweep: "+n+" invoice(s) updated.");
    }
}
