package com.sebastianhauss.videoplatform.domain.worker;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Worker {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String instanceId;

    private String hostname;

    private WorkerStatus status;

    private Instant lastHeartbeat;

    private Instant startedAt;

    public void onCreate() {
        this.startedAt = Instant.now();
        this.lastHeartbeat = Instant.now();
        this.status = WorkerStatus.ONLINE;
    }
}
