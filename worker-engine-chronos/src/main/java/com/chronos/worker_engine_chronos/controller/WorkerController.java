package com.chronos.worker_engine_chronos.controller;

import com.chronos.worker_engine_chronos.config.RabbitMqConfig;
import com.chronos.worker_engine_chronos.model.Job;
import com.chronos.worker_engine_chronos.model.JobExecutionLog;
import com.chronos.worker_engine_chronos.model.JobStatus;
import com.chronos.worker_engine_chronos.repository.JobExecutionLogRepository;
import com.chronos.worker_engine_chronos.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/worker")
@RequiredArgsConstructor
public class WorkerController {

    private final JobRepository jobRepository;
    private final JobExecutionLogRepository logRepository;
    private final RabbitTemplate rabbitTemplate;

    // List jobs, optionally by status
    @GetMapping("/jobs")
    public ResponseEntity<List<Job>> listJobs(@RequestParam(required = false) JobStatus status) {
        List<Job> jobs = (status == null)
                ? jobRepository.findAll()
                : jobRepository.findByStatus(status);
        return ResponseEntity.ok(jobs);
    }

    // Get a single job by id
    @GetMapping("/jobs/{id}")
    public ResponseEntity<Job> getJob(@PathVariable Long id) {
        Optional<Job> job = jobRepository.findById(id);
        return job.map(ResponseEntity::ok)
                  .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // Manually requeue a job for execution
    @PostMapping("/jobs/{id}/requeue")
    public ResponseEntity<Void> requeue(@PathVariable Long id) {
        if (jobRepository.existsById(id)) {
            rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE_NAME, RabbitMqConfig.ROUTING_KEY, Map.of("id", id));
            return ResponseEntity.accepted().build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    // Alias for requeue: run-now
    @PostMapping("/jobs/{id}/run-now")
    public ResponseEntity<Void> runNow(@PathVariable Long id) {
        return requeue(id);
    }

    // Fetch execution logs, optionally for a specific job
    @GetMapping("/logs")
    public ResponseEntity<List<JobExecutionLog>> logs(@RequestParam(required = false) Long jobId) {
        List<JobExecutionLog> logs = (jobId == null)
                ? logRepository.findAll()
                : logRepository.findByJobIdOrderByStartedAtDesc(jobId);
        return ResponseEntity.ok(logs);
    }

    // Quick status stats across all jobs
    @GetMapping("/stats")
    public ResponseEntity<Map<JobStatus, Long>> stats() {
        Map<JobStatus, Long> map = new EnumMap<>(JobStatus.class);
        for (JobStatus s : JobStatus.values()) {
            map.put(s, jobRepository.countByStatus(s));
        }
        return ResponseEntity.ok(map);
    }
}
