package com.chronos.worker_engine_chronos.repository;

import com.chronos.worker_engine_chronos.model.Job;
import com.chronos.worker_engine_chronos.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByStatus(JobStatus status);

    long countByStatus(JobStatus status);
}
