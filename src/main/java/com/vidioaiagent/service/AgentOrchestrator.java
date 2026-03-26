package com.vidioaiagent.service;

import com.vidioaiagent.dto.request.AdGenerateRequest;
import com.vidioaiagent.dto.response.AdCopyResponse;
import com.vidioaiagent.dto.response.AdProjectResponse;
import com.vidioaiagent.dto.response.TrendAnalysisResponse;
import com.vidioaiagent.dto.response.VideoGenerateResponse;
import com.vidioaiagent.entity.AdProject;
import com.vidioaiagent.enums.ProjectStatus;
import com.vidioaiagent.repository.AdProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final TrendAnalysisService trendAnalysisService;
    private final AdCopyService adCopyService;
    private final VideoGenerateService videoGenerateService;
    private final ThumbnailService thumbnailService;
    private final AdProjectRepository adProjectRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public AdProjectResponse createAndStart(AdGenerateRequest request) {
        AdProject project = AdProject.builder()
                .productName(request.getProductName())
                .productDescription(request.getProductDescription())
                .targetAudience(request.getTargetAudience())
                .platform(request.getPlatform())
                .adStyle(request.getAdStyle())
                .additionalRequest(request.getAdditionalRequest())
                .status(ProjectStatus.PENDING)
                .progressPercent(0)
                .build();
        project = adProjectRepository.save(project);

        orchestrate(project.getId(), request);
        return toResponse(project);
    }

    @Async
    public void orchestrate(Long projectId, AdGenerateRequest request) {
        log.info("=== 파이프라인 시작 === 프로젝트 ID: {}", projectId);

        try {
            // Step 1: 트렌드 분석
            updateProgress(projectId, ProjectStatus.TREND_ANALYZING, 10);
            TrendAnalysisResponse trendData = trendAnalysisService.analyzeTrend(request);
            saveTrendResult(projectId, trendData);
            updateProgress(projectId, ProjectStatus.TREND_ANALYZING, 25);
            log.info("[Step 1] 트렌드 분석 완료");

            // Step 2: 씬 스크립트 생성
            updateProgress(projectId, ProjectStatus.COPY_GENERATING, 30);
            AdCopyResponse adCopy = adCopyService.generateAdCopy(request, trendData);
            saveCopyAndScript(projectId, adCopy);
            updateProgress(projectId, ProjectStatus.COPY_GENERATING, 50);
            log.info("[Step 2] 씬 스크립트 생성 완료 - {}개 씬",
                    adCopy.getScenes() != null ? adCopy.getScenes().size() : 0);

            // Step 3: 영상 + 썸네일 생성
            updateProgress(projectId, ProjectStatus.VIDEO_GENERATING, 55);
            VideoGenerateResponse videoResult = videoGenerateService.generateVideo(adCopy, request);
            saveVideoResult(projectId, videoResult);
            updateProgress(projectId, ProjectStatus.VIDEO_GENERATING, 85);
            log.info("[Step 3] 영상 생성 완료 - status: {}", videoResult.getStatus());

            // Step 4: AI 썸네일 (DALL-E, 있으면 덮어쓰기)
            updateProgress(projectId, ProjectStatus.THUMBNAIL_GENERATING, 90);
            try {
                String aiThumbnail = thumbnailService.generateThumbnail(
                        request.getProductName(),
                        adCopy.getTitle(),
                        request.getAdStyle().name()
                );
                if (aiThumbnail != null) {
                    saveThumbnailResult(projectId, aiThumbnail);
                    log.info("[Step 4] AI 썸네일 생성 완료");
                }
            } catch (Exception e) {
                log.info("[Step 4] AI 썸네일 미사용 - Java2D 썸네일 유지");
            }

            // 완료
            updateProgress(projectId, ProjectStatus.COMPLETED, 100);
            log.info("=== 파이프라인 완료 === 프로젝트 ID: {}", projectId);

        } catch (Exception e) {
            log.error("=== 파이프라인 실패 === ID: {}, 오류: {}", projectId, e.getMessage(), e);
            adProjectRepository.findById(projectId).ifPresent(project -> {
                project.setStatus(ProjectStatus.FAILED);
                project.setErrorMessage(e.getMessage());
                adProjectRepository.save(project);
            });
            sendProgress(projectId, ProjectStatus.FAILED, 0);
        }
    }

    public AdProjectResponse getProjectResult(Long projectId) {
        AdProject project = adProjectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("프로젝트를 찾을 수 없습니다: " + projectId));
        return toResponse(project);
    }

    public List<AdProjectResponse> getAllProjects() {
        return adProjectRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public AdProjectResponse retryStep(Long projectId, String step) {
        AdProject project = adProjectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("프로젝트를 찾을 수 없습니다"));

        log.info("단계 재실행 - 프로젝트: {}, 단계: {}", projectId, step);

        AdGenerateRequest request = AdGenerateRequest.builder()
                .productName(project.getProductName())
                .productDescription(project.getProductDescription())
                .targetAudience(project.getTargetAudience())
                .platform(project.getPlatform())
                .adStyle(project.getAdStyle())
                .additionalRequest(project.getAdditionalRequest())
                .build();

        project.setStatus(ProjectStatus.PENDING);
        project.setErrorMessage(null);
        adProjectRepository.save(project);
        orchestrate(projectId, request);
        return toResponse(project);
    }

    // ─── 저장 ───

    private void saveTrendResult(Long projectId, TrendAnalysisResponse trendData) {
        try {
            String json = objectMapper.writeValueAsString(trendData);
            adProjectRepository.findById(projectId).ifPresent(p -> {
                p.setTrendAnalysis(json);
                p.setHookText(trendData.getHookSuggestion());
                adProjectRepository.save(p);
            });
        } catch (Exception e) {
            log.error("트렌드 저장 실패", e);
        }
    }

    private void saveCopyAndScript(Long projectId, AdCopyResponse adCopy) {
        try {
            String copyJson = objectMapper.writeValueAsString(adCopy);
            String subtitlesJson = objectMapper.writeValueAsString(adCopy.toSubtitles());

            adProjectRepository.findById(projectId).ifPresent(p -> {
                p.setAdCopy(copyJson);
                p.setScript(adCopy.toScript());
                p.setSubtitles(subtitlesJson);
                p.setHookText(adCopy.getHook());
                if (adCopy.getHashtags() != null) {
                    p.setHashtags(String.join(" ", adCopy.getHashtags()));
                }
                adProjectRepository.save(p);
            });
        } catch (Exception e) {
            log.error("카피/스크립트 저장 실패", e);
        }
    }

    private void saveVideoResult(Long projectId, VideoGenerateResponse videoResult) {
        adProjectRepository.findById(projectId).ifPresent(p -> {
            if (videoResult.getVideoUrl() != null) p.setVideoUrl(videoResult.getVideoUrl());
            if (videoResult.getThumbnailUrl() != null) p.setThumbnailUrl(videoResult.getThumbnailUrl());
            adProjectRepository.save(p);
        });
    }

    private void saveThumbnailResult(Long projectId, String thumbnailFileName) {
        adProjectRepository.findById(projectId).ifPresent(p -> {
            p.setThumbnailUrl(thumbnailFileName);
            adProjectRepository.save(p);
        });
    }

    // ─── 변환 ───

    private AdProjectResponse toResponse(AdProject p) {
        return AdProjectResponse.builder()
                .id(p.getId())
                .productName(p.getProductName())
                .productDescription(p.getProductDescription())
                .targetAudience(p.getTargetAudience())
                .platform(p.getPlatform())
                .adStyle(p.getAdStyle())
                .status(p.getStatus())
                .progressPercent(p.getProgressPercent())
                .trendAnalysis(p.getTrendAnalysis())
                .adCopy(p.getAdCopy())
                .script(p.getScript())
                .hashtags(p.getHashtags())
                .hookText(p.getHookText())
                .subtitles(p.getSubtitles())
                .videoUrl(p.getVideoUrl())
                .thumbnailUrl(p.getThumbnailUrl())
                .errorMessage(p.getErrorMessage())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private void updateProgress(Long projectId, ProjectStatus status, int percent) {
        adProjectRepository.findById(projectId).ifPresent(p -> {
            p.setStatus(status);
            p.setProgressPercent(percent);
            adProjectRepository.save(p);
        });
        sendProgress(projectId, status, percent);
    }

    private void sendProgress(Long projectId, ProjectStatus status, int percent) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("projectId", projectId);
        payload.put("status", status.name());
        payload.put("progressPercent", percent);
        payload.put("message", status.getDisplayName());
        messagingTemplate.convertAndSend("/topic/progress/" + projectId, (Object) payload);
    }
}
