package com.chronos.job_api_chronos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username; // Set from JWT, not from client

    @NotBlank(message = "jobType is required")
    @Column(nullable = false)
    private String jobType; // e.g., "SEND_EMAIL", "DATA_SYNC"

    @NotBlank(message = "payload is required")
    @Column(columnDefinition = "TEXT")
    private String payload; // e.g., '{"to": "test@test.com"}'

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    private String cronExpression; // For recurring jobs (e.g., "0 0 * * *")
    private boolean isRecurring;

    private int retryCount;
    private int maxRetries;

    private LocalDateTime createdAt;
    private LocalDateTime scheduledAt; // For future execution
    private LocalDateTime lastExecutedAt; // For recurring jobs tracking
    private LocalDateTime nextExecutionAt; // For recurring jobs

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.maxRetries == 0) this.maxRetries = 3;
    }
}