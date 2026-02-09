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
        job.setStartedAt(Instant.now());
        jobRepository.save(job);
    }

    @Transactional
    public void markAsFailed(ProcessingJob job, String errorMessage) {
        job.setStatus(ProcessingJobStatus.FAILED);
        job.setFinishedAt(Instant.now());
        job.setErrorMessage(errorMessage);
        job.setRetryCount(job.getRetryCount() + 1);
        jobRepository.save(job);
    }

    @Transactional
    public void retryJob(ProcessingJob job) {
        job.setStatus(ProcessingJobStatus.PENDING);
        job.setStartedAt(null);
        job.setFinishedAt(null);
        job.setErrorMessage(null);
        jobRepository.save(job);
    }

    public List<ProcessingJob> getPendingJobs() {
        return jobRepository.findPendingJobsWithRetries(ProcessingJobStatus.PENDING, 3);
    }
}
