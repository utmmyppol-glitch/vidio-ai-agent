package com.vidioaiagent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidioaiagent.dto.request.AdGenerateRequest;
import com.vidioaiagent.dto.response.TrendAnalysisResponse;
import com.vidioaiagent.enums.Platform;
import com.vidioaiagent.enums.Style;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrendAnalysisServiceTest {

    @Mock private ChatClient chatClient;
    private TrendAnalysisService trendAnalysisService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        trendAnalysisService = new TrendAnalysisService(chatClient, objectMapper);
    }

    private AdGenerateRequest sampleRequest() {
        return AdGenerateRequest.builder()
                .productName("프로틴 바")
                .targetAudience("30대 남성 직장인")
                .platform(Platform.INSTAGRAM_REELS)
                .adStyle(Style.INFORMATIVE)
                .additionalRequest("헬스 관련")
                .build();
    }

    @Test
    @DisplayName("정상 JSON 응답 파싱 성공")
    void analyzeTrend_validJson_success() {
        String mockJsonResponse = """
                {
                    "trendKeywords": ["프로틴", "헬스", "단백질"],
                    "competitorAnalysis": "경쟁사는 감성 톤 위주",
                    "viralPoints": ["운동 전후 비교", "성분 강조"],
                    "recommendedHashtags": ["#프로틴", "#헬스"],
                    "contentDirection": "팩트 기반 성분 비교",
                    "hookSuggestion": "프로틴 바 하나로 단백질 30g?"
                }
                """;

        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(mockJsonResponse);

        TrendAnalysisResponse result = trendAnalysisService.analyzeTrend(sampleRequest());

        assertThat(result.getTrendKeywords()).containsExactly("프로틴", "헬스", "단백질");
        assertThat(result.getViralPoints()).hasSize(2);
        assertThat(result.getHookSuggestion()).contains("프로틴 바");
        assertThat(result.getRecommendedHashtags()).contains("#프로틴");
    }

    @Test
    @DisplayName("```json 코드블록 감싸진 응답도 파싱 성공")
    void analyzeTrend_codeBlockWrapped_success() {
        String wrappedResponse = """
                ```json
                {
                    "trendKeywords": ["키워드1"],
                    "competitorAnalysis": "분석",
                    "viralPoints": ["포인트1"],
                    "recommendedHashtags": ["#태그"],
                    "contentDirection": "방향",
                    "hookSuggestion": "후킹"
                }
                ```
                """;

        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(wrappedResponse);

        TrendAnalysisResponse result = trendAnalysisService.analyzeTrend(sampleRequest());

        assertThat(result.getTrendKeywords()).containsExactly("키워드1");
    }

    @Test
    @DisplayName("잘못된 JSON 응답 시 RuntimeException")
    void analyzeTrend_invalidJson_throws() {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("이건 JSON이 아닙니다");

        assertThatThrownBy(() -> trendAnalysisService.analyzeTrend(sampleRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("파싱에 실패");
    }
}
