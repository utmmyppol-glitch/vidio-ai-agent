package com.vidioaiagent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidioaiagent.dto.request.AdGenerateRequest;
import com.vidioaiagent.dto.response.AdCopyResponse;
import com.vidioaiagent.dto.response.AdProjectResponse;
import com.vidioaiagent.dto.response.TrendAnalysisResponse;
import com.vidioaiagent.dto.response.VideoGenerateResponse;
import com.vidioaiagent.entity.AdProject;
import com.vidioaiagent.enums.Platform;
import com.vidioaiagent.enums.ProjectStatus;
import com.vidioaiagent.enums.Style;
import com.vidioaiagent.repository.AdProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentOrchestratorTest {

    @Mock private TrendAnalysisService trendAnalysisService;
    @Mock private AdCopyService adCopyService;
    @Mock private VideoGenerateService videoGenerateService;
    @Mock private AdProjectRepository adProjectRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AgentOrchestrator agentOrchestrator;

    private AdGenerateRequest request;
    private AdProject savedProject;

    @BeforeEach
    void setUp() {
        request = AdGenerateRequest.builder()
                .productName("다이어트 음료")
                .targetAudience("20대 여성")
                .platform(Platform.YOUTUBE_SHORTS)
                .adStyle(Style.PROVOCATIVE)
                .build();

        savedProject = AdProject.builder()
                .id(1L)
                .productName("다이어트 음료")
                .targetAudience("20대 여성")
                .platform(Platform.YOUTUBE_SHORTS)
                .adStyle(Style.PROVOCATIVE)
                .status(ProjectStatus.PENDING)
                .progressPercent(0)
                .build();
    }

    @Test
    @DisplayName("createAndStart - 프로젝트 생성 후 응답 반환")
    void createAndStart_success() {
        when(adProjectRepository.save(any(AdProject.class))).thenReturn(savedProject);

        AdProjectResponse response = agentOrchestrator.createAndStart(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getProductName()).isEqualTo("다이어트 음료");
        assertThat(response.getPlatform()).isEqualTo(Platform.YOUTUBE_SHORTS);
        assertThat(response.getAdStyle()).isEqualTo(Style.PROVOCATIVE);
        assertThat(response.getStatus()).isEqualTo(ProjectStatus.PENDING);
        verify(adProjectRepository).save(any(AdProject.class));
    }

    @Test
    @DisplayName("getProjectResult - 존재하는 프로젝트 조회")
    void getProjectResult_success() {
        when(adProjectRepository.findById(1L)).thenReturn(Optional.of(savedProject));

        AdProjectResponse response = agentOrchestrator.getProjectResult(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getProductName()).isEqualTo("다이어트 음료");
    }

    @Test
    @DisplayName("getProjectResult - 존재하지 않는 프로젝트 조회 시 예외")
    void getProjectResult_notFound() {
        when(adProjectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agentOrchestrator.getProjectResult(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("프로젝트를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("getAllProjects - 전체 목록 조회")
    void getAllProjects_success() {
        when(adProjectRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(savedProject));

        List<AdProjectResponse> responses = agentOrchestrator.getAllProjects();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getProductName()).isEqualTo("다이어트 음료");
    }

    @Test
    @DisplayName("orchestrate - 전체 파이프라인 성공")
    void orchestrate_fullPipeline_success() {
        TrendAnalysisResponse trendData = TrendAnalysisResponse.builder()
                .trendKeywords(List.of("다이어트", "제로칼로리"))
                .viralPoints(List.of("비포애프터"))
                .recommendedHashtags(List.of("#다이어트"))
                .contentDirection("변화 강조")
                .hookSuggestion("이거 마시면 진짜 빠진다")
                .competitorAnalysis("경쟁사 분석")
                .build();

        AdCopyResponse adCopy = AdCopyResponse.builder()
                .script("스크립트 내용")
                .title("제목")
                .description("설명")
                .hashtags(List.of("#다이어트", "#제로"))
                .subtitles(List.of(
                        AdCopyResponse.SubtitleEntry.builder()
                                .startTime(0.0).endTime(3.0).text("자막1").build()))
                .thumbnailText("썸네일")
                .build();

        VideoGenerateResponse videoResult = VideoGenerateResponse.builder()
                .videoUrl(null)
                .thumbnailUrl(null)
                .uploadText("업로드 텍스트")
                .status("SCRIPT_READY")
                .build();

        when(adProjectRepository.findById(1L)).thenReturn(Optional.of(savedProject));
        when(trendAnalysisService.analyzeTrend(any())).thenReturn(trendData);
        when(adCopyService.generateAdCopy(any(), any())).thenReturn(adCopy);
        when(videoGenerateService.generateVideo(any(), any())).thenReturn(videoResult);

        agentOrchestrator.orchestrate(1L, request);

        verify(trendAnalysisService).analyzeTrend(any());
        verify(adCopyService).generateAdCopy(any(), any());
        verify(videoGenerateService).generateVideo(any(), any());
    }

    @Test
    @DisplayName("orchestrate - 트렌드 분석 실패 시 FAILED 상태")
    void orchestrate_trendFail_setsFailed() {
        when(adProjectRepository.findById(1L)).thenReturn(Optional.of(savedProject));
        when(trendAnalysisService.analyzeTrend(any()))
                .thenThrow(new RuntimeException("API 호출 실패"));

        agentOrchestrator.orchestrate(1L, request);

        assertThat(savedProject.getStatus()).isEqualTo(ProjectStatus.FAILED);
        assertThat(savedProject.getErrorMessage()).contains("API 호출 실패");
    }
}
