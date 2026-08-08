package com.sebastianhauss.videoplatform.processing;

import com.sebastianhauss.videoplatform.domain.processing.ProcessingJob;
import com.sebastianhauss.videoplatform.domain.video.Video;
import com.sebastianhauss.videoplatform.dto.video.ProcessedVideo;
import com.sebastianhauss.videoplatform.service.ProcessingJobService;
import com.sebastianhauss.videoplatform.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Polls for pending {@link ProcessingJob}s and runs them off the request
 * thread. Single-instance: the scheduler is single-threaded and {@code
 * fixedDelay} prevents overlapping runs, so no job is claimed twice. Scaling to
 * multiple instances would need row-level job claiming (e.g. SELECT ... FOR
 * UPDATE SKIP LOCKED).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VideoProcessingWorker {

    private final ProcessingJobService jobService;
    private final VideoService videoService;
    private final VideoProcessor videoProcessor;

    @Scheduled(fixedDelayString = "${processing.poll-interval-ms:5000}")
    public void pollPendingJobs() {
        List<ProcessingJob> jobs = jobService.getPendingJobs();
        if (jobs.isEmpty()) {
            return;
        }
        log.debug("Picked up {} pending processing job(s)", jobs.size());
        for (ProcessingJob job : jobs) {
            runJob(job);
        }
    }

    private void runJob(ProcessingJob job) {
        UUID videoId = job.getVideo().getId();
        try {
            jobService.markAsRunning(job);
            Video video = videoService.markProcessing(videoId);

            ProcessedVideo result = videoProcessor.process(video);

            videoService.markReady(videoId, result);
            jobService.markAsCompleted(job);
            log.info("Processed video {}", videoId);

        } catch (Exception e) {
            log.error("Processing failed for video {} (job {})", videoId, job.getId(), e);
            jobService.recordFailure(job, e.getMessage());
            if (jobService.isExhausted(job)) {
                videoService.markFailed(videoId);
            }
        }
    }
}
