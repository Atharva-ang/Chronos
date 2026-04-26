package com.chronos.job_api_chronos.service;

import com.chronos.job_api_chronos.config.RabbitMqConfig;
import com.chronos.job_api_chronos.exception.NotFoundException;
import com.chronos.job_api_chronos.model.Job;
import com.chronos.job_api_chronos.model.JobStatus;
import com.chronos.job_api_chronos.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final RabbitTemplate rabbitTemplate;

    // Original-style API
    public Job createJob(Job jobRequest, String username) {
        LocalDateTime now = LocalDateTime.now();

        Job newJob = Job.builder()
                .username(username)
                .jobType(jobRequest.getJobType())
                .payload(jobRequest.getPayload())
                .status(JobStatus.INQUEUE)
                .cronExpression(jobRequest.getCronExpression())
                .isRecurring(jobRequest.isRecurring())
                .scheduledAt(jobRequest.getScheduledAt())
                .build();

        // For recurring jobs, calculate the first execution time
        if (newJob.isRecurring() && newJob.getCronExpression() != null) {
            try {
                CronExpression cron = CronExpression.parse(newJob.getCronExpression());
                var next = cron.next(now.atZone(ZoneId.systemDefault()));
                if (next != null) {
                    newJob.setNextExecutionAt(next.toLocalDateTime());
                    log.info("Recurring job will first execute at: {}", newJob.getNextExecutionAt());
                }
            } catch (Exception e) {
                log.error("Invalid cron expression: {}", newJob.getCronExpression(), e);
                throw new IllegalArgumentException("Invalid cron expression: " + newJob.getCronExpression());
            }
        }

        Job savedJob = jobRepository.save(newJob);

        // Only publish immediately if it's not scheduled for the future and not recurring
        if (!savedJob.isRecurring() && (savedJob.getScheduledAt() == null || !savedJob.getScheduledAt().isAfter(now))) {
            log.info("Publishing job {} immediately to queue", savedJob.getId());
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.EXCHANGE_NAME,
                    RabbitMqConfig.ROUTING_KEY,
                    Map.of("id", savedJob.getId())
            );
        } else if (savedJob.getScheduledAt() != null) {
            log.info("Job {} scheduled for execution at {}", savedJob.getId(), savedJob.getScheduledAt());
        } else if (savedJob.isRecurring()) {
            log.info("Recurring job {} created with cron: {}", savedJob.getId(), savedJob.getCronExpression());
        }

        return savedJob;
    }

    public List<Job> getJobs(String username, JobStatus status) {
        if (status != null) {
            return jobRepository.findByStatusAndUsername(status, username);
        }
        return jobRepository.findByUsernameOrderByCreatedAtDesc(username);
    }

    public Job getJobById(Long id, String username) {
        return jobRepository.findByIdAndUsername(id, username)
                .orElseThrow(() -> new NotFoundException("Job not found or unauthorized"));
    }

    public void cancelJob(Long id, String username) {
        Job job = getJobById(id, username);
        if (job.getStatus() == JobStatus.INQUEUE) {
            job.setStatus(JobStatus.CANCELLED);
            jobRepository.save(job);
        } else {
            throw new IllegalStateException("Too late! Cannot cancel job. Current status is: " + job.getStatus());
        }
    }

    public Job rescheduleJob(Long id, String username, String newCronExpression) {
        Job job = getJobById(id, username);
        if (job.getStatus() == JobStatus.INQUEUE || job.getStatus() == JobStatus.CANCELLED) {
            job.setCronExpression(newCronExpression);
            job.setStatus(JobStatus.INQUEUE);
            return jobRepository.save(job);
        } else {
            throw new IllegalStateException("Cannot reschedule a job that is currently " + job.getStatus());
        }
    }

}