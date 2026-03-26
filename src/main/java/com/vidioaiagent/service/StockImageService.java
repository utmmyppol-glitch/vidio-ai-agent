package com.vidioaiagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Pexels API 기반 스톡 미디어(영상/이미지) 다운로드.
 */
@Slf4j
@Service
public class StockImageService {

    @Value("${app.pexels.api-key:}")
    private String pexelsApiKey;

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final ObjectMapper om = new ObjectMapper();

    /** 세션 내 이미 사용한 영상 ID를 추적하여 중복 방지 */
    private final Set<Integer> usedVideoIds = new HashSet<>();

    /** 새 프로젝트 시작 시 사용 기록 초기화 */
    public void resetUsedVideos() {
        usedVideoIds.clear();
    }

    public boolean hasPexelsKey() {
        return pexelsApiKey != null && !pexelsApiKey.isBlank();
    }

    // ══════════════════════════════════════
    //  스톡 영상 다운로드 (Pexels Video API)
    // ══════════════════════════════════════

    /**
     * 키워드로 Pexels 스톡 영상을 검색해서 다운로드한다.
     * @param keyword   검색어 (예: "고양이 놀라는 장면")
     * @param orientation "portrait" or "landscape"
     * @param outputPath 저장 경로 (.mp4)
     * @return 성공 여부
     */
    public boolean downloadVideo(String keyword, String orientation, Path outputPath) {
        if (!hasPexelsKey()) return false;

        try {
            String query = sanitize(keyword);
            if (query.isBlank()) query = "aesthetic background";

            String url = String.format(
                    "https://api.pexels.com/videos/search?query=%s&orientation=%s&per_page=10&size=small",
                    URLEncoder.encode(query, StandardCharsets.UTF_8), orientation
            );

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", pexelsApiKey)
                    .timeout(Duration.ofSeconds(10))
                    .GET().build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("Pexels Video API: HTTP {}", resp.statusCode());
                return false;
            }

            JsonNode root = om.readTree(resp.body());
            JsonNode videos = root.get("videos");
            if (videos == null || videos.isEmpty()) {
                log.info("Pexels 영상 없음: '{}' → 영어로 재시도", query);
                // 한글 키워드일 경우 핵심 단어만 추출해서 재시도는 caller에서 처리
                return false;
            }

            // 관련도 점수 기반 최적 영상 선택
            JsonNode bestVideo = pickMostRelevantVideo(videos, query);
            if (bestVideo == null) {
                // 모든 영상이 이미 사용됨 → fallback: 단순화된 키워드로 재검색
                String simplified = simplifyKeyword(query);
                if (!simplified.equals(query)) {
                    log.info("모든 영상 사용됨, 단순화 키워드로 재시도: '{}'", simplified);
                    return downloadVideo(simplified, orientation, outputPath);
                }
                return false;
            }

            int videoId = bestVideo.has("id") ? bestVideo.get("id").asInt() : 0;
            usedVideoIds.add(videoId);

            JsonNode files = bestVideo.get("video_files");
            if (files == null || files.isEmpty()) return false;

            // HD 영상 우선, 없으면 SD
            String videoUrl = pickBestVideoUrl(files, orientation);
            if (videoUrl == null) return false;

            return downloadFile(videoUrl, outputPath);

        } catch (Exception e) {
            log.warn("Pexels 영상 다운로드 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 검색 결과에서 키워드 관련도가 가장 높은 영상을 선택한다.
     * 이미 사용된 영상은 건너뛴다.
     */
    private JsonNode pickMostRelevantVideo(JsonNode videos, String keyword) {
        Set<String> keywordWords = new HashSet<>(
                Arrays.asList(keyword.toLowerCase().split("\\s+"))
        );

        JsonNode bestVideo = null;
        int bestRelevance = -1;

        for (JsonNode video : videos) {
            int videoId = video.has("id") ? video.get("id").asInt() : 0;
            if (usedVideoIds.contains(videoId)) continue;

            int relevance = 0;

            // tags 배열에서 매칭 (Pexels Video API에서 제공되는 경우)
            if (video.has("tags") && video.get("tags").isArray()) {
                for (JsonNode tag : video.get("tags")) {
                    String tagText = tag.isTextual() ? tag.asText().toLowerCase() : "";
                    for (String kw : keywordWords) {
                        if (tagText.contains(kw)) relevance++;
                    }
                }
            }

            // URL에서 키워드 매칭 (tags가 없는 경우 대비 보조 시그널)
            String videoUrl = video.has("url") ? video.get("url").asText().toLowerCase() : "";
            for (String kw : keywordWords) {
                if (kw.length() >= 3 && videoUrl.contains(kw)) relevance++;
            }

            if (relevance > bestRelevance) {
                bestRelevance = relevance;
                bestVideo = video;
            }
        }

        // 관련도 0이면 fallback: 아직 사용 안 된 첫 번째 영상 반환
        if (bestVideo == null) return null;
        return bestVideo;
    }

    /**
     * 키워드를 단순화한다 (첫 번째 핵심 단어만 남김).
     */
    private String simplifyKeyword(String keyword) {
        String[] words = keyword.trim().split("\\s+");
        if (words.length <= 1) return keyword;
        // 첫 두 단어만 사용
        return words[0] + " " + words[1];
    }

    /**
     * 최적 화질 영상 URL 선택 (HD > SD, orientation 매칭 우선)
     */
    private String pickBestVideoUrl(JsonNode files, String orientation) {
        String best = null;
        int bestScore = -1;
        boolean wantVertical = "portrait".equals(orientation);

        for (JsonNode f : files) {
            int w = f.has("width") ? f.get("width").asInt() : 0;
            int h = f.has("height") ? f.get("height").asInt() : 0;
            String quality = f.has("quality") ? f.get("quality").asText() : "";
            String link = f.has("link") ? f.get("link").asText() : null;
            if (link == null) continue;

            int score = 0;
            // 해상도 점수
            if ("hd".equals(quality)) score += 10;
            else if ("sd".equals(quality)) score += 5;

            // orientation 매칭
            boolean isVertical = h > w;
            if (isVertical == wantVertical) score += 20;

            // 적절한 크기 (너무 크면 다운로드 느림)
            if (w >= 720 && w <= 1920) score += 5;

            if (score > bestScore) {
                bestScore = score;
                best = link;
            }
        }
        return best;
    }

    // ══════════════════════════════════════
    //  스톡 이미지 다운로드 (폴백용)
    // ══════════════════════════════════════

    public boolean downloadImage(String keyword, int width, int height, Path outputPath) {
        if (hasPexelsKey()) {
            if (downloadImageFromPexels(keyword, width, height, outputPath)) return true;
        }
        return downloadFromPicsum(keyword, width, height, outputPath);
    }

    private boolean downloadImageFromPexels(String keyword, int width, int height, Path outputPath) {
        try {
            String orientation = height > width ? "portrait" : "landscape";
            String query = sanitize(keyword);
            if (query.isBlank()) query = "abstract background";

            String url = String.format(
                    "https://api.pexels.com/v1/search?query=%s&orientation=%s&per_page=5&size=medium",
                    URLEncoder.encode(query, StandardCharsets.UTF_8), orientation
            );

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", pexelsApiKey)
                    .timeout(Duration.ofSeconds(10))
                    .GET().build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return false;

            JsonNode photos = om.readTree(resp.body()).get("photos");
            if (photos == null || photos.isEmpty()) return false;

            int idx = (int) (Math.random() * Math.min(photos.size(), 5));
            String imgUrl = height > width
                    ? photos.get(idx).get("src").get("portrait").asText()
                    : photos.get(idx).get("src").get("landscape").asText();

            return downloadFile(imgUrl, outputPath);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean downloadFromPicsum(String keyword, int width, int height, Path outputPath) {
        try {
            int seed = Math.abs(keyword.hashCode()) % 1000;
            String url = String.format("https://picsum.photos/seed/%d/%d/%d", seed, width, height);
            return downloadFile(url, outputPath);
        } catch (Exception e) {
            return false;
        }
    }

    // ══════════════════════════════════════
    //  유틸
    // ══════════════════════════════════════

    private boolean downloadFile(String url, Path outputPath) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET().build();

            HttpResponse<InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() == 200) {
                try (InputStream is = resp.body()) {
                    Files.copy(is, outputPath, StandardCopyOption.REPLACE_EXISTING);
                }
                log.info("다운로드 완료: {} ({}KB)", outputPath.getFileName(), Files.size(outputPath) / 1024);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("다운로드 실패: {}", e.getMessage());
            return false;
        }
    }

    private String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9가-힣\\s]", "").trim();
    }
}
