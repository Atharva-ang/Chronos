package com.chronos.worker_engine_chronos;

import com.chronos.worker_engine_chronos.model.Job;
import com.chronos.worker_engine_chronos.model.JobStatus;
import com.chronos.worker_engine_chronos.service.JobExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

@ExtendWith(MockitoExtension.class)
class JobExecutorTest {

    @InjectMocks
    private JobExecutor jobExecutor;

    @Test
    void execute_sendEmail_doesNotThrow() {
        Job job = Job.builder().id(1L).jobType("SEND_EMAIL").payload("{}").status(JobStatus.INQUEUE).build();

        assertThatNoException().isThrownBy(() -> jobExecutor.execute(job));
    }

    @Test
    void execute_dataSync_doesNotThrow() {
        Job job = Job.builder().id(2L).jobType("DATA_SYNC").payload("{}").status(JobStatus.INQUEUE).build();

        assertThatNoException().isThrownBy(() -> jobExecutor.execute(job));
    }

    @Test
    void execute_unknownJobType_throwsIllegalArgumentException() {
        Job job = Job.builder().id(3L).jobType("UNKNOWN_TYPE").payload("{}").status(JobStatus.INQUEUE).build();

        assertThatThrownBy(() -> jobExecutor.execute(job))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported job type");
    }
}