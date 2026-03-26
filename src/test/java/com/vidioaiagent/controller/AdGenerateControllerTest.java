package com.vidioaiagent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vidioaiagent.config.SecurityConfig;
import com.vidioaiagent.dto.request.AdGenerateRequest;
import com.vidioaiagent.dto.response.AdProjectResponse;
import com.vidioaiagent.enums.Platform;
import com.vidioaiagent.enums.ProjectStatus;
import com.vidioaiagent.enums.Style;
import com.vidioaiagent.service.AgentOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdGenerateController.class)
@Import(SecurityConfig.class)
@AutoConfigureJsonTesters
class AdGenerateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgentOrchestrator agentOrchestrator;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private AdProjectResponse sampleResponse() {
        return AdProjectResponse.builder()
                .id(1L)
                .productName("테스트 상품")
                .targetAudience("20대 여성")
                .platform(Platform.YOUTUBE_SHORTS)
                .adStyle(Style.EMOTIONAL)
                .status(ProjectStatus.PENDING)
                .progressPercent(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private AdGenerateRequest sampleRequest() {
        return AdGenerateRequest.builder()
                .productName("테스트 상품")
                .targetAudience("20대 여성")
                .platform(Platform.YOUTUBE_SHORTS)
                .adStyle(Style.EMOTIONAL)
                .build();
    }

    @Test
    @DisplayName("POST /api/projects - 프로젝트 생성 성공")
    void createProject_success() throws Exception {
        when(agentOrchestrator.createAndStart(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.productName").value("테스트 상품"))
                .andExpect(jsonPath("$.data.platform").value("YOUTUBE_SHORTS"));
    }

    @Test
    @DisplayName("POST /api/projects - 필수값 누락 시 400 에러")
    void createProject_validation_fail() throws Exception {
        AdGenerateRequest invalid = AdGenerateRequest.builder()
                .productName("")
                .build();

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/projects/{id} - 프로젝트 조회 성공")
    void getProject_success() throws Exception {
        when(agentOrchestrator.getProjectResult(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/projects - 전체 목록 조회")
    void getAllProjects_success() throws Exception {
        when(agentOrchestrator.getAllProjects()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("GET /api/projects/options - 옵션 목록 조회")
    void getOptions_success() throws Exception {
        mockMvc.perform(get("/api/projects/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.platforms").isArray())
                .andExpect(jsonPath("$.data.adStyles").isArray())
                .andExpect(jsonPath("$.data.platforms[0].value").value("YOUTUBE_SHORTS"))
                .andExpect(jsonPath("$.data.adStyles[0].value").value("EMOTIONAL"));
    }

    @Test
    @DisplayName("POST /api/projects/{id}/retry - 재실행 요청")
    void retryStep_success() throws Exception {
        AdProjectResponse retrying = sampleResponse();
        retrying.setStatus(ProjectStatus.PENDING);
        when(agentOrchestrator.retryStep(1L, "TREND_ANALYZING")).thenReturn(retrying);

        mockMvc.perform(post("/api/projects/1/retry")
                        .param("step", "TREND_ANALYZING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
