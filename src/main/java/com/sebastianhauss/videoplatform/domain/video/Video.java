package com.sebastianhauss.videoplatform.domain.video;

import com.sebastianhauss.videoplatform.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "videos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Video {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String bucket;

    @Column(nullable = false, unique = true)
    private String objectKey;

    @Column(name = "thumbnail_key")
    private String thumbnailKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoVisibility visibility;

    private Long sizeBytes;

    @Column(name = "duration_millis")
    private Long durationMillis;

    private String contentType;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
