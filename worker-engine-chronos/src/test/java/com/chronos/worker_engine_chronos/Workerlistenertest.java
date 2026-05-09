package com.chronos.worker_engine_chronos;

import com.chronos.worker_engine_chronos.model.Job;
import com.chronos.worker_engine_chronos.model.JobStatus;
import com.chronos.worker_engine_chronos.repository.JobExecutionLogRepository;
import com.chronos.worker_engine_chronos.repository.JobRepository;
import com.chronos.worker_engine_chronos.service.JobExecutor;
import com.chronos.worker_engine_chronos.service.NotificationService;
import com.chronos.worker_engine_chronos.service.WorkerListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerListenerTest {

    @Mock private JobRepository jobRepository;
    @Mock private JobExecutionLogRepository logRepository;
    @Mock private JobExecutor jobExecutor;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private NotificationService notificationService;

    @InjectMocks private WorkerListener workerListener;

    private Job sampleJob;

    @BeforeEach
    void setUp() {
        sampleJob = Job.builder()
                .id(1L)
                .username("testuser")
                .jobType("SEND_EMAIL")
                .payload("{}")
                .status(JobStatus.INQUEUE)
                .retryCount(0)
                .maxRetries(3)
                .build();
    }

    @Test
    void onNewJob_validJob_executesAndCompletesSuccessfully() throws Exception {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        workerListener.onNewJob("{\"id\": 1}");

        assertThat(sampleJob.getStatus()).isEqualTo(JobStatus.COMPLETED);
        verify(jobExecutor).execute(sampleJob);
        verify(notificationService).notifyJobSuccess(sampleJob);
    }

    @Test
    void onNewJob_cancelledJob_skipsExecution() throws Exception {
        sampleJob.setStatus(JobStatus.CANCELLED);
        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));

        workerListener.onNewJob("{\"id\": 1}");

        verify(jobExecutor, never()).execute(any());
    }

    @Test
    void onNewJob_alreadyInProgress_skipsExecution() throws Exception {
        sampleJob.setStatus(JobStatus.INPROGRESS);
        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));

        workerListener.onNewJob("{\"id\": 1}");

        verify(jobExecutor, never()).execute(any());
    }

    @Test
    void onNewJob_jobNotFound_skipsExecution() throws Exception {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        workerListener.onNewJob("{\"id\": 99}");

        verify(jobExecutor, never()).execute(any());
    }

    @Test
    void onNewJob_invalidJson_doesNotThrow() throws Exception {
        workerListener.onNewJob("NOT_JSON");
        verify(jobExecutor, never()).execute(any());
    }

    @Test
    void onNewJob_executionFails_retriesJob() throws Exception {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("Simulated failure")).when(jobExecutor).execute(any());

        workerListener.onNewJob("{\"id\": 1}");

        assertThat(sampleJob.getStatus()).isEqualTo(JobStatus.INQUEUE);
        assertThat(sampleJob.getRetryCount()).isEqualTo(1);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Map.class));
    }

    @Test
    void onNewJob_executionFailsExceedsRetries_marksAsFailed() throws Exception {
        sampleJob.setRetryCount(2); // one away from maxRetries (3)
        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("Final failure")).when(jobExecutor).execute(any());

        workerListener.onNewJob("{\"id\": 1}");

        assertThat(sampleJob.getStatus()).isEqualTo(JobStatus.FAILED);
        verify(notificationService).notifyJobFailure(eq(sampleJob), anyString());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Map.class));
    }
}