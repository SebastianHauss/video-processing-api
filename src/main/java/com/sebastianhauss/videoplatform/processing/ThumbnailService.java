package com.sebastianhauss.videoplatform.processing;

import com.sebastianhauss.videoplatform.config.SystemFFmpegLocator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Service
@RequiredArgsConstructor
public class ThumbnailService {

    private final SystemFFmpegLocator ffmpegLocator;

    public File generateThumbnail(File videoFile) throws EncoderException, IOException {
        File outputFile = Files.createTempFile("thumb-", ".png").toFile();

        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setOutputFormat("image2");
        attrs.setOffset(3f);
        attrs.setDuration(0.1f);

        VideoAttributes video = new VideoAttributes();
        video.setCodec("png");
        video.setFrameRate(1);

        attrs.setVideoAttributes(video);
        attrs.setAudioAttributes(null);

        new Encoder(ffmpegLocator).encode(new MultimediaObject(videoFile, ffmpegLocator), outputFile, attrs);
        return outputFile;
    }
}
