package com.vidioaiagent.service;

import com.vidioaiagent.dto.request.AdGenerateRequest;
import com.vidioaiagent.dto.response.TrendAnalysisResponse;
import com.vidioaiagent.util.PromptTemplates;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrendAnalysisService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public TrendAnalysisResponse analyzeTrend(AdGenerateRequest request) {
        log.info("트렌드 분석 시작 - 상품: {}, 타겟: {}, 플랫폼: {}",
                request.getProductName(), request.getTargetAudience(), request.getPlatform());

        String additionalText = request.getAdditionalRequest() != null
                ? "- 추가 정보: " + request.getAdditionalRequest()
                : "";

        String prompt = PromptTemplates.TREND_ANALYSIS
                .replace("{productName}", request.getProductName())
                .replace("{targetAudience}", request.getTargetAudience())
                .replace("{platform}", request.getPlatform().getDisplayName())
                .replace("{style}", request.getAdStyle().getDisplayName())
                .replace("{additionalInfo}", additionalText);

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        try {
            String jsonContent = extractJson(response);
            TrendAnalysisResponse result = objectMapper.readValue(jsonContent, TrendAnalysisResponse.class);
            log.info("트렌드 분석 완료 - 키워드 {}개, 해시태그 {}개",
                    result.getTrendKeywords().size(), result.getRecommendedHashtags().size());
            return result;
        } catch (Exception e) {
            log.error("트렌드 분석 응답 파싱 실패: {}", e.getMessage());
            throw new RuntimeException("트렌드 분석 결과 파싱에 실패했습니다", e);
        }
    }

    private String extractJson(String response) {
        if (response == null) return "{}";
        String s = response.trim();

        // ```json ... ``` 블록 추출
        int jsonBlock = s.indexOf("```json");
        if (jsonBlock >= 0) {
            int start = s.indexOf('\n', jsonBlock) + 1;
            int end = s.indexOf("```", start);
            if (end > start) return s.substring(start, end).trim();
        }
        int codeBlock = s.indexOf("```");
        if (codeBlock >= 0) {
            int start = s.indexOf('\n', codeBlock) + 1;
            int end = s.indexOf("```", start);
            if (end > start) return s.substring(start, end).trim();
        }

        // { ... } 블록 직접 추출
        int braceStart = s.indexOf('{');
        int braceEnd = s.lastIndexOf('}');
        if (braceStart >= 0 && braceEnd > braceStart) {
            return s.substring(braceStart, braceEnd + 1);
        }

        return s;
    }
}
