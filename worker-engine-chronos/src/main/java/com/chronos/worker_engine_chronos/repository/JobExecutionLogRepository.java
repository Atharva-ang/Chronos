package com.chronos.worker_engine_chronos.repository;

import com.chronos.worker_engine_chronos.model.JobExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobExecutionLogRepository extends JpaRepository<JobExecutionLog, Long> {
    List<JobExecutionLog> findByJobIdOrderByStartedAtDesc(Long jobId);
}
