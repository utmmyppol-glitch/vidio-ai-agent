package com.vidioaiagent.controller;

import com.vidioaiagent.dto.request.AdGenerateRequest;
import com.vidioaiagent.dto.response.AdProjectResponse;
import com.vidioaiagent.dto.response.ApiResponse;
import com.vidioaiagent.dto.response.ProjectOptions;
import com.vidioaiagent.enums.Platform;
import com.vidioaiagent.enums.Style;
import com.vidioaiagent.service.AgentOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Ad Project", description = "광고 프로젝트 생성 및 관리 API")
public class AdGenerateController {

    private final AgentOrchestrator agentOrchestrator;

    @PostMapping
    @Operation(summary = "프로젝트 생성", description = "상품 정보를 입력하면 트렌드 분석 → 카피 생성 → 영상 생성 파이프라인이 비동기로 시작됩니다")
    public ResponseEntity<ApiResponse<AdProjectResponse>> createProject(@Valid @RequestBody AdGenerateRequest request) {
        log.info("프로젝트 생성 요청 - 상품: {}, 플랫폼: {}", request.getProductName(), request.getPlatform());
        AdProjectResponse response = agentOrchestrator.createAndStart(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "프로젝트가 생성되었습니다"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "프로젝트 조회", description = "프로젝트 ID로 현재 상태와 결과를 조회합니다 (폴링용)")
    public ResponseEntity<ApiResponse<AdProjectResponse>> getProject(
            @Parameter(description = "프로젝트 ID") @PathVariable Long id) {
        AdProjectResponse response = agentOrchestrator.getProjectResult(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    @Operation(summary = "전체 프로젝트 목록", description = "생성된 모든 프로젝트를 최신순으로 조회합니다")
    public ResponseEntity<ApiResponse<List<AdProjectResponse>>> getAllProjects() {
        List<AdProjectResponse> responses = agentOrchestrator.getAllProjects();
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "단계 재실행", description = "실패한 파이프라인 단계를 재실행합니다")
    public ResponseEntity<ApiResponse<AdProjectResponse>> retryStep(
            @Parameter(description = "프로젝트 ID") @PathVariable Long id,
            @Parameter(description = "재실행할 단계 (TREND_ANALYZING, COPY_GENERATING 등)") @RequestParam String step) {
        log.info("단계 재실행 요청 - 프로젝트: {}, 단계: {}", id, step);
        AdProjectResponse response = agentOrchestrator.retryStep(id, step);
        return ResponseEntity.ok(ApiResponse.ok(response, "재실행이 시작되었습니다"));
    }

    @GetMapping("/options")
    @Operation(summary = "옵션 목록 조회", description = "플랫폼, 스타일 등 선택 가능한 옵션 목록을 반환합니다")
    public ResponseEntity<ApiResponse<ProjectOptions>> getOptions() {
        List<ProjectOptions.PlatformOption> platforms = Arrays.stream(Platform.values())
                .map(p -> ProjectOptions.PlatformOption.builder()
                        .value(p.name())
                        .label(p.getDisplayName())
                        .aspectRatio(p.getAspectRatio())
                        .maxDuration(p.getMaxDurationSeconds())
                        .build())
                .toList();

        List<ProjectOptions.AdStyleOption> adStyles = Arrays.stream(Style.values())
                .map(s -> ProjectOptions.AdStyleOption.builder()
                        .value(s.name())
                        .label(s.getDisplayName())
                        .description(s.getDescription())
                        .build())
                .toList();

        ProjectOptions options = ProjectOptions.builder()
                .platforms(platforms)
                .adStyles(adStyles)
                .build();

        return ResponseEntity.ok(ApiResponse.ok(options));
    }
}
