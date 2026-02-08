package com.sebastianhauss.videoplatform.repository;

import com.sebastianhauss.videoplatform.domain.processing.ProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, UUID> {
}
