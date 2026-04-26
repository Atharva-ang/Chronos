package com.chronos.worker_engine_chronos.service;

import com.chronos.worker_engine_chronos.model.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmailNotificationService emailNotificationService;

    /**
     * Notify user when a job fails permanently
     */
    public void notifyJobFailure(Job job, String errorMessage) {
        log.info("=== JOB FAILURE NOTIFICATION ===");
        log.info("User: {}", job.getUsername());
        log.info("Job ID: {}", job.getId());
        log.info("Job Type: {}", job.getJobType());
        log.info("Payload: {}", job.getPayload());
        log.info("Error: {}", errorMessage);
        log.info("Retry Attempts: {}/{}", job.getRetryCount(), job.getMaxRetries());
        log.info("================================");

        // Send email notification
        emailNotificationService.sendJobFailureEmail(job, errorMessage);

        // TODO: Additional notification channels can be added here:
        // - Send webhook to user-configured URL
        // - Push to notification service (SNS, Firebase, etc.)
        // - Store in notifications table for user to view in UI
    }

    /**
     * Notify user when a job completes successfully (optional)
     */
    public void notifyJobSuccess(Job job) {
        log.debug("Job {} completed successfully for user {}", job.getId(), job.getUsername());
        // Optional: Implement success notifications if needed
    }

    /**
     * Notify user when a recurring job execution fails
     */
    public void notifyRecurringJobIssue(Job job, String message) {
        log.warn("=== RECURRING JOB ISSUE ===");
        log.warn("User: {}", job.getUsername());
        log.warn("Job ID: {}", job.getId());
        log.warn("Cron: {}", job.getCronExpression());
        log.warn("Issue: {}", message);
        log.warn("===========================");
    }
}
