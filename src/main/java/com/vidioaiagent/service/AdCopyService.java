package com.vidioaiagent.service;

import com.vidioaiagent.dto.request.AdGenerateRequest;
import com.vidioaiagent.dto.response.AdCopyResponse;
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
public class AdCopyService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public AdCopyResponse generateAdCopy(AdGenerateRequest request, TrendAnalysisResponse trendData) {
        log.info("숏폼 씬 스크립트 생성 시작 - 상품: {}, 플랫폼: {}, 스타일: {}",
                request.getProductName(),
                request.getPlatform().getDisplayName(),
                request.getAdStyle().getDisplayName());

        String prompt = PromptTemplates.AD_COPY_GENERATION
                .replace("{productName}", safe(request.getProductName()))
                .replace("{productDescription}", safe(request.getProductDescription()))
                .replace("{targetAudience}", safe(request.getTargetAudience()))
                .replace("{platform}", request.getPlatform().getDisplayName())
                .replace("{maxDuration}", String.valueOf(request.getPlatform().getMaxDurationSeconds()))
                .replace("{style}", request.getAdStyle().getDisplayName())
                .replace("{styleDescription}", request.getAdStyle().getDescription())
                .replace("{trendKeywords}", String.join(", ", trendData.getTrendKeywords()))
                .replace("{viralPoints}", String.join(", ", trendData.getViralPoints()))
                .replace("{contentDirection}", safe(trendData.getContentDirection()))
                .replace("{hookSuggestion}", safe(trendData.getHookSuggestion()));

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        try {
            String jsonContent = extractJson(response);
            AdCopyResponse result = objectMapper.readValue(jsonContent, AdCopyResponse.class);

            // scenes → subtitles, script 하위 호환 세팅
            if (result.getScenes() != null && !result.getScenes().isEmpty()) {
                result.setSubtitles(result.toSubtitles());
                result.setScript(result.toScript());
            }

            log.info("씬 스크립트 생성 완료 - {}개 씬, 해시태그 {}개",
                    result.getScenes() != null ? result.getScenes().size() : 0,
                    result.getHashtags() != null ? result.getHashtags().size() : 0);
            return result;
        } catch (Exception e) {
            log.error("응답 파싱 실패. 원본 응답:\n{}", response.substring(0, Math.min(response.length(), 500)));
            throw new RuntimeException("광고 카피 생성 결과 파싱 실패", e);
        }
    }

    private String extractJson(String response) {
        String trimmed = response.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    private String safe(String s) {
        return s != null ? s : "";
    }
}
