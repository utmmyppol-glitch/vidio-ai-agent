package com.vidioaiagent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
@Service
public class ThumbnailService {

    private final OpenAiImageModel imageModel;

    @Value("${app.content.thumbnail-dir:./generated-content/thumbnails}")
    private String thumbnailDir;

    public ThumbnailService(@Autowired(required = false) OpenAiImageModel imageModel) {
        this.imageModel = imageModel;
        if (imageModel == null) {
            log.warn("OpenAI ImageModel이 설정되지 않았습니다. 썸네일 생성이 비활성화됩니다.");
        }
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Path.of(thumbnailDir));
        } catch (Exception e) {
            log.warn("썸네일 디렉토리 생성 실패: {}", e.getMessage());
        }
    }

    /**
     * DALL-E를 사용해서 광고 카피 기반 썸네일 이미지를 생성한다.
     *
     * @param productName 상품명
     * @param adTitle     광고 타이틀
     * @param style       광고 스타일
     * @return 생성된 파일명 or null
     */
    public String generateThumbnail(String productName, String adTitle, String style) {
        if (imageModel == null) {
            log.info("OpenAI 키 미설정 — 썸네일 생성 건너뜀 (상품: {})", productName);
            return null;
        }

        log.info("썸네일 생성 시작 - 상품: {}", productName);

        try {
            String prompt = buildThumbnailPrompt(productName, adTitle, style);

            ImageResponse response = imageModel.call(new ImagePrompt(prompt));

            String imageUrl = response.getResult().getOutput().getUrl();
            if (imageUrl == null) {
                log.warn("DALL-E 응답에 URL이 없습니다");
                return null;
            }

            // 이미지 다운로드 후 저장
            String fileName = UUID.randomUUID() + ".png";
            Path outputPath = Path.of(thumbnailDir, fileName);

            try (InputStream in = URI.create(imageUrl).toURL().openStream()) {
                Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("썸네일 생성 완료: {}", fileName);
            return fileName;

        } catch (Exception e) {
            log.error("썸네일 생성 실패: {}", e.getMessage());
            return null;
        }
    }

    private String buildThumbnailPrompt(String productName, String adTitle, String style) {
        return String.format(
            "Create a visually stunning YouTube thumbnail image for an advertisement. " +
            "Product: %s. " +
            "Ad headline: %s. " +
            "Style: %s, modern, eye-catching. " +
            "The image should be vibrant, professional, and designed to maximize click-through rate. " +
            "Do NOT include any text or letters in the image. Focus on visual impact only.",
            productName, adTitle != null ? adTitle : productName, style
        );
    }
}
