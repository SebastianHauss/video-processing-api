package com.sebastianhauss.videoplatform.repository;

import com.sebastianhauss.videoplatform.domain.processing.ProcessingJob;
import com.sebastianhauss.videoplatform.domain.processing.ProcessingJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, UUID> {

    List<ProcessingJob> findByStatusOrderByCreatedAtDesc(ProcessingJobStatus status);

    List<ProcessingJob> findByVideoIdOrderByCreatedAtDesc(UUID videoId);

    @Query("SELECT pj FROM ProcessingJob pj WHERE pj.status = :status AND pj.retryCount < :maxRetries")
    List<ProcessingJob> findPendingJobsWithRetries(
            @Param("status") ProcessingJobStatus status,
            @Param("maxRetries") int maxRetries
    );
}
