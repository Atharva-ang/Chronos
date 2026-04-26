package com.chronos.worker_engine_chronos.service;

import com.chronos.worker_engine_chronos.model.Job;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JobExecutor {

    public void execute(Job job) throws Exception {
        String type = job.getJobType();
        switch (type) {
            case "SEND_EMAIL" -> simulateEmail(job);
            case "DATA_SYNC" -> simulateDataSync(job);
            default -> {
                log.warn("Unknown jobType '{}', marking as failed", type);
                throw new IllegalArgumentException("Unsupported job type: " + type);
            }
        }
    }

    private void simulateEmail(Job job) throws InterruptedException {
        log.info("Simulating email send for job {} with payload {}", job.getId(), job.getPayload());
        Thread.sleep(300); // simulate work
    }

    private void simulateDataSync(Job job) throws InterruptedException {
        log.info("Simulating data sync for job {} with payload {}", job.getId(), job.getPayload());
        Thread.sleep(500); // simulate work
    }
}
