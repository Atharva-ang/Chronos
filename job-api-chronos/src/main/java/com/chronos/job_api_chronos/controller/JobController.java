package com.chronos.job_api_chronos.controller;

import com.chronos.job_api_chronos.model.Job;
import com.chronos.job_api_chronos.model.JobStatus;
import com.chronos.job_api_chronos.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    private String getAuthenticatedUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @PostMapping
    public ResponseEntity<Job> submitJob(@Valid @RequestBody Job jobRequest) {
        Job savedJob = jobService.createJob(jobRequest, getAuthenticatedUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedJob);
    }

    @GetMapping
    public ResponseEntity<List<Job>> getJobs(@RequestParam(required = false) JobStatus status) {
        List<Job> jobs = jobService.getJobs(getAuthenticatedUsername(), status);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable Long id) {
        Job job = jobService.getJobById(id, getAuthenticatedUsername());
        return ResponseEntity.ok(job);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<String> cancelJob(@PathVariable Long id) {
        jobService.cancelJob(id, getAuthenticatedUsername());
        return ResponseEntity.ok("Job successfully cancelled.");
    }

    @PutMapping("/{id}/reschedule")
    public ResponseEntity<Job> rescheduleJob(@PathVariable Long id, @RequestParam String cronExpression) {
        Job updatedJob = jobService.rescheduleJob(id, getAuthenticatedUsername(), cronExpression);
        return ResponseEntity.ok(updatedJob);
    }
}