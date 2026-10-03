package com.example.labsupport.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs the SLA check in the background. Default: every 60 seconds (app.sla.check-interval-ms). */
@Component
public class SlaScheduler {

    private static final Logger log = LoggerFactory.getLogger(SlaScheduler.class);

    private final SlaService slaService;

    public SlaScheduler(SlaService slaService) {
        this.slaService = slaService;
    }

    @Scheduled(fixedDelayString = "${app.sla.check-interval-ms:60000}", initialDelay = 30000)
    public void checkSla() {
        try {
            int flagged = slaService.markOverdueTickets();
            if (flagged > 0) {
                log.info("SLA check: {} ticket(s) flagged as breached", flagged);
            }
        } catch (RuntimeException ex) {
            log.error("SLA check failed", ex);
        }
    }
}
