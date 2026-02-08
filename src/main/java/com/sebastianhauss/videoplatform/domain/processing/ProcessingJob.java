package com.sebastianhauss.videoplatform.domain.processing;

import com.sebastianhauss.videoplatform.domain.video.Video;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processing_jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingJob {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingJobType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingJobStatus status;

    private Instant startedAt;
    private Instant finishedAt;

    @Column(length = 2000)
    private String errorMessage;

    private int retryCount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }
}
