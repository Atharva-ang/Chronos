package com.chronos.job_api_chronos.service;

import com.chronos.job_api_chronos.config.RabbitMqConfig;
import com.chronos.job_api_chronos.model.Job;
import com.chronos.job_api_chronos.model.JobStatus;
import com.chronos.job_api_chronos.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobSchedulerService {

    private final JobRepository jobRepository;
    private final RabbitTemplate rabbitTemplate;

    // 🔥 prevents overlapping executions
    private final AtomicBoolean isRunningScheduled = new AtomicBoolean(false);
    private final AtomicBoolean isRunningRecurring = new AtomicBoolean(false);

    @Scheduled(fixedDelay = 30000)
    public void processScheduledJobs() {

        if (!isRunningScheduled.compareAndSet(false, true)) {
            log.warn("⚠️ Skipping scheduled job run (already running)");
            return;
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            log.info("🔁 Scheduled poll at {} | Thread: {}", now, Thread.currentThread().getName());

            List<Job> scheduledJobs =
                    jobRepository.findByStatusAndScheduledAtBefore(JobStatus.INQUEUE, now);

            // 🔥 LIMIT manually (safety if repo not limited)
            int limit = Math.min(scheduledJobs.size(), 10);

            for (int i = 0; i < limit; i++) {
                Job job = scheduledJobs.get(i);

                if (!job.isRecurring()) {
                    try {
                        log.info("Publishing scheduled job {}", job.getId());

                        publishJobToQueue(job);

                        job.setStatus(JobStatus.INPROGRESS);
                        jobRepository.save(job);

                    } catch (Exception e) {
                        log.error("Error publishing job {}: {}", job.getId(), e.getMessage(), e);
                    }
                }
            }

        } finally {
            isRunningScheduled.set(false);
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void processRecurringJobs() {

        if (!isRunningRecurring.compareAndSet(false, true)) {
            log.warn("⚠️ Skipping recurring job run (already running)");
            return;
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            log.info("🔁 Recurring poll at {} | Thread: {}", now, Thread.currentThread().getName());

            List<Job> recurringJobs = jobRepository.findRecurringJobsDueForExecution(now);

            // 🔥 LIMIT
            int limit = Math.min(recurringJobs.size(), 10);

            for (int i = 0; i < limit; i++) {
                Job job = recurringJobs.get(i);

                if (job.getStatus() == JobStatus.CANCELLED) continue;

                try {
                    LocalDateTime nextExecution =
                            calculateNextExecution(job.getCronExpression(), now);

                    if (nextExecution == null) continue;

                    if (job.getNextExecutionAt() == null || !job.getNextExecutionAt().isAfter(now)) {

                        log.info("Publishing recurring job {}", job.getId());

                        publishJobToQueue(job);

                        job.setStatus(JobStatus.INPROGRESS);
                        job.setLastExecutedAt(now);
                        job.setNextExecutionAt(nextExecution);

                        jobRepository.save(job);
                    }

                } catch (Exception e) {
                    log.error("Error processing recurring job {}: {}", job.getId(), e.getMessage(), e);
                }
            }

        } finally {
            isRunningRecurring.set(false);
        }
    }

    private LocalDateTime calculateNextExecution(String cronExpression, LocalDateTime from) {
        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            var next = cron.next(from.atZone(ZoneId.systemDefault()));
            return next != null ? next.toLocalDateTime() : null;
        } catch (Exception e) {
            log.error("Failed to parse cron '{}': {}", cronExpression, e.getMessage());
            return null;
        }
    }

    private void publishJobToQueue(Job job) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE_NAME,
                RabbitMqConfig.ROUTING_KEY,
                Map.of("id", job.getId())
        );
    }
}