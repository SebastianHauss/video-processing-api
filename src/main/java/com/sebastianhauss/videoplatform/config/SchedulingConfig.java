package com.sebastianhauss.videoplatform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables the background scheduler that drives the video processing pipeline
 * (see {@code VideoProcessingWorker}).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
