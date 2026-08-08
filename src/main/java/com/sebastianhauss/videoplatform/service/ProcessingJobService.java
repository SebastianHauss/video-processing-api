package com.sebastianhauss.videoplatform.service;

import com.sebastianhauss.videoplatform.domain.processing.ProcessingJob;
import com.sebastianhauss.videoplatform.domain.processing.ProcessingJobStatus;
import com.sebastianhauss.videoplatform.domain.processing.ProcessingJobType;
import com.sebastianhauss.videoplatform.domain.video.Video;
import com.sebastianhauss.videoplatform.repository.ProcessingJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessingJobService {

    private static final int MAX_RETRIES = 3;

    private final ProcessingJobRepository jobRepository;

    public ProcessingJob createJob(Video video, ProcessingJobType type) {
        ProcessingJob job = ProcessingJob.builder()
                .video(video)
                .type(type)
                .status(ProcessingJobStatus.PENDING)
                .retryCount(0)
                .build();

        return jobRepository.save(job);
    }

    @Transactional
    public void markAsRunning(ProcessingJob job) {
        job.setStatus(ProcessingJobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        jobRepository.save(job);
    }

    @Transactional
    public void markAsCompleted(ProcessingJob job) {
        job.setStatus(ProcessingJobStatus.COMPLETED);
        job.setFinishedAt(Instant.now());
        jobRepository.save(job);
    }

    /**
     * Records a failed attempt. Requeues the job as PENDING for another attempt
     * until {@link #MAX_RETRIES} is reached, after which it stays FAILED.
     */
    @Transactional
    public void recordFailure(ProcessingJob job, String errorMessage) {
        job.setRetryCount(job.getRetryCount() + 1);
        job.setErrorMessage(errorMessage);
        job.setFinishedAt(Instant.now());

        if (job.getRetryCount() >= MAX_RETRIES) {
            job.setStatus(ProcessingJobStatus.FAILED);
        } else {
            job.setStatus(ProcessingJobStatus.PENDING);
            job.setStartedAt(null);
        }
        jobRepository.save(job);
    }

    public boolean isExhausted(ProcessingJob job) {
        return job.getRetryCount() >= MAX_RETRIES;
    }

    public List<ProcessingJob> getPendingJobs() {
        return jobRepository.findPendingJobsWithRetries(ProcessingJobStatus.PENDING, MAX_RETRIES);
    }
}
