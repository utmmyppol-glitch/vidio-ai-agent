package com.vidioaiagent.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(AnthropicChatModel anthropicChatModel) {
        return ChatClient.builder(anthropicChatModel)
                .defaultSystem("당신은 트렌드 분석과 광고 콘텐츠 제작 전문 AI 에이전트입니다. " +
                        "항상 한국어로 응답하며, 최신 트렌드와 바이럴 콘텐츠에 대한 깊은 이해를 바탕으로 " +
                        "효과적인 광고 카피와 영상 스크립트를 생성합니다.")
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.ai.openai.api-key", matchIfMissing = false)
    public OpenAiImageModel openAiImageModel(
            @Value("${spring.ai.openai.api-key}") String apiKey) {
        OpenAiImageApi imageApi = OpenAiImageApi.builder().apiKey(apiKey).build();
        return new OpenAiImageModel(imageApi);
    }
}
