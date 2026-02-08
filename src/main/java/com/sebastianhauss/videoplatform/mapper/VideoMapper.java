package com.sebastianhauss.videoplatform.mapper;

import com.sebastianhauss.videoplatform.domain.video.Video;
import com.sebastianhauss.videoplatform.dto.video.VideoResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VideoMapper {

    VideoResponse toResponse(Video video);

    List<VideoResponse> toResponseList(List<Video> videos);
}
