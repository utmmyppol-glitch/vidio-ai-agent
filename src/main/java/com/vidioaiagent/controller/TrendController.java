package com.vidioaiagent.controller;

import com.vidioaiagent.dto.request.AdGenerateRequest;
import com.vidioaiagent.dto.response.ApiResponse;
import com.vidioaiagent.dto.response.TrendAnalysisResponse;
import com.vidioaiagent.service.TrendAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/trend")
@RequiredArgsConstructor
@Tag(name = "Trend Analysis", description = "트렌드 분석 단독 실행 API")
public class TrendController {

    private final TrendAnalysisService trendAnalysisService;

    @PostMapping("/analyze")
    @Operation(summary = "트렌드 분석", description = "상품 정보를 기반으로 현재 트렌드 키워드, 경쟁 분석, 바이럴 포인트를 분석합니다")
    public ResponseEntity<ApiResponse<TrendAnalysisResponse>> analyzeTrend(@Valid @RequestBody AdGenerateRequest request) {
        log.info("트렌드 분석 단독 요청 - 상품: {}", request.getProductName());
        TrendAnalysisResponse response = trendAnalysisService.analyzeTrend(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
