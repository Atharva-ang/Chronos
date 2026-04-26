package com.chronos.job_api_chronos.repository;

import com.chronos.job_api_chronos.model.Job;
import com.chronos.job_api_chronos.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByUsernameOrderByCreatedAtDesc(String username);
    List<Job> findByStatusAndUsername(JobStatus status, String username);
    Optional<Job> findByIdAndUsername(Long id, String username);

    // Find scheduled jobs that are due for execution
    List<Job> findByStatusAndScheduledAtBefore(JobStatus status, LocalDateTime time);

    // Find recurring jobs that need to be executed
    @Query("SELECT j FROM Job j WHERE j.isRecurring = true AND j.status != 'CANCELLED' " +
           "AND (j.nextExecutionAt IS NULL OR j.nextExecutionAt <= :now)")
    List<Job> findRecurringJobsDueForExecution(@Param("now") LocalDateTime now);
}
