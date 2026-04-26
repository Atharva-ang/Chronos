package com.chronos.worker_engine_chronos.model;

import jakarta.persistence.*;
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
    private String username;

    @Column(nullable = false)
    private String jobType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    private String cronExpression;
    private boolean isRecurring;

    private int retryCount;
    private int maxRetries;

    private LocalDateTime createdAt;
    private LocalDateTime scheduledAt; // For future execution
    private LocalDateTime lastExecutedAt; // For recurring jobs tracking
    private LocalDateTime nextExecutionAt; // For recurring jobs

    @PrePersist
    protected void onCreate() {
        if (this.maxRetries == 0) this.maxRetries = 3;
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }
}
