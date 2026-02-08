package com.sebastianhauss.videoplatform.repository;

import com.sebastianhauss.videoplatform.domain.worker.Worker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkerRepository extends JpaRepository<Worker, UUID> {
}
