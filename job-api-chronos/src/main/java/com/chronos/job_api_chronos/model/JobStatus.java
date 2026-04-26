package com.chronos.job_api_chronos.model;

public enum JobStatus {
    INQUEUE,     // Sitting in RabbitMQ waiting for a worker
    INPROGRESS,  // Worker is currently executing the code
    COMPLETED,   // Finished successfully
    FAILED,      // Crashed and exceeded maxRetries
    CANCELLED    // User manually stopped it via API
}