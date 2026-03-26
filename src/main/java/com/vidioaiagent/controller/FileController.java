package com.vidioaiagent.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;

@Slf4j
@RestController
@RequestMapping("/api/files")
public class FileController {

    @Value("${app.content.video-dir:./generated-content/videos}")
    private String videoDir;

    @Value("${app.content.thumbnail-dir:./generated-content/thumbnails}")
    private String thumbnailDir;

    /**
     * 영상 파일 다운로드/스트리밍
     * GET /api/files/video/{fileName}
     */
    @GetMapping("/video/{fileName}")
    public ResponseEntity<Resource> getVideo(@PathVariable String fileName) {
        File file = Path.of(videoDir, fileName).toFile();
        if (!file.exists()) {
            log.warn("영상 파일 없음: {}", file.getAbsolutePath());
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(new FileSystemResource(file));
    }

    /**
     * 썸네일 이미지
     * GET /api/files/thumbnail/{fileName}
     */
    @GetMapping("/thumbnail/{fileName}")
    public ResponseEntity<Resource> getThumbnail(@PathVariable String fileName) {
        File file = Path.of(thumbnailDir, fileName).toFile();
        if (!file.exists()) {
            log.warn("썸네일 파일 없음: {}", file.getAbsolutePath());
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(new FileSystemResource(file));
    }

    /**
     * 장면 이미지 (이미지 시퀀스)
     * GET /api/files/scene/{seqId}/{fileName}
     */
    @GetMapping("/scene/{seqId}/{fileName}")
    public ResponseEntity<Resource> getSceneImage(
            @PathVariable String seqId, @PathVariable String fileName) {
        File file = Path.of(videoDir, seqId, fileName).toFile();
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(new FileSystemResource(file));
    }

    /**
     * 장면 메타 정보
     * GET /api/files/scene/{seqId}/meta.json
     */
    @GetMapping("/scene/{seqId}/meta")
    public ResponseEntity<Resource> getSceneMeta(@PathVariable String seqId) {
        File file = Path.of(videoDir, seqId, "meta.json").toFile();
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new FileSystemResource(file));
    }
}
