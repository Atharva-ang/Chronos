package com.chronos.worker_engine_chronos.service;

import com.chronos.worker_engine_chronos.config.RabbitMqConfig;
import com.chronos.worker_engine_chronos.model.Job;
import com.chronos.worker_engine_chronos.model.JobExecutionLog;
import com.chronos.worker_engine_chronos.model.JobStatus;
import com.chronos.worker_engine_chronos.repository.JobExecutionLogRepository;
import com.chronos.worker_engine_chronos.repository.JobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerListener {

    private final JobRepository jobRepository;
    private final JobExecutionLogRepository logRepository;
    private final JobExecutor jobExecutor;
    private final RabbitTemplate rabbitTemplate;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(
            queues = RabbitMqConfig.QUEUE_NAME,
            concurrency = "1"
    )
    public void onNewJob(String rawPayload) {
        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(rawPayload, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse message payload: {}", rawPayload, e);
            return;
        }

        try {
            Long jobId = extractJobId(payload);
            if (jobId == null) {
                log.warn("Received job message without an id: {}", payload);
                return;
            }

            Optional<Job> optionalJob = jobRepository.findById(jobId);
            if (optionalJob.isEmpty()) {
                log.warn("Job {} not found in DB; skipping", jobId);
                return;
            }

            Job job = optionalJob.get();

            if (job.getStatus() == JobStatus.INPROGRESS) {
                log.warn("Job {} already in progress, skipping duplicate", jobId);
                return;
            }

            if (job.getStatus() == JobStatus.CANCELLED) {
                log.info("Job {} is CANCELLED, skipping execution.", jobId);
                return;
            }

            job.setStatus(JobStatus.INPROGRESS);
            jobRepository.save(job);

            JobExecutionLog execLog = JobExecutionLog.builder()
                    .jobId(job.getId())
                    .status(JobStatus.INPROGRESS)
                    .startedAt(LocalDateTime.now())
                    .build();
            execLog = logRepository.save(execLog);

            log.info("🚀 START EXECUTION {}", jobId);
            jobExecutor.execute(job);
            log.info("✅ END EXECUTION {}", jobId);

            job.setStatus(JobStatus.COMPLETED);
            job.setRetryCount(0);
            jobRepository.save(job);

            execLog.setStatus(JobStatus.COMPLETED);
            execLog.setFinishedAt(LocalDateTime.now());
            logRepository.save(execLog);

            log.info("Job {} completed successfully", jobId);
            notificationService.notifyJobSuccess(job);

        } catch (Exception e) {
            handleFailure(payload, e);
        }
    }

    private void handleFailure(Map<String, Object> payload, Exception e) {
        Long jobId = extractJobId(payload);
        if (jobId == null) {
            log.error("Failed handling job payload (no id). Error: {}", e.getMessage(), e);
            return;
        }

        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.error("Job {} not found during failure handling", jobId, e);
            return;
        }

        JobExecutionLog execLog = JobExecutionLog.builder()
                .jobId(job.getId())
                .status(JobStatus.FAILED)
                .startedAt(LocalDateTime.now())
                .finishedAt(LocalDateTime.now())
                .errorMessage(e.getMessage())
                .build();
        logRepository.save(execLog);

        int nextRetry = job.getRetryCount() + 1;
        job.setRetryCount(nextRetry);

        if (nextRetry < job.getMaxRetries()) {
            job.setStatus(JobStatus.INQUEUE);
            jobRepository.save(job);

            try {
                long delay = (long) Math.pow(2, nextRetry) * 1000;
                log.warn("Retrying job {} in {}ms (attempt {}/{})", jobId, delay, nextRetry, job.getMaxRetries());
                Thread.sleep(delay);
            } catch (InterruptedException ignored) {}

            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.EXCHANGE_NAME,
                    RabbitMqConfig.ROUTING_KEY,
                    Map.of("id", jobId)
            );

        } else {
            job.setStatus(JobStatus.FAILED);
            jobRepository.save(job);

            log.error("Job {} failed permanently after {} attempts. Error: {}",
                    jobId, nextRetry, e.getMessage());

            notificationService.notifyJobFailure(job, e.getMessage());
        }
    }

    private Long extractJobId(Map<String, Object> payload) {
        Object idObj = payload.get("id");
        if (idObj instanceof Number n) {
            return n.longValue();
        }
        return null;
    }
}