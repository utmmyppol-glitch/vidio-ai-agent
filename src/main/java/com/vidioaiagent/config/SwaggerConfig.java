package com.vidioaiagent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TrendAd AI Agent API")
                        .version("1.0.0")
                        .description("트렌드 분석 → 광고 카피 → 영상 자동 생성 AI Agent API")
                        .contact(new Contact()
                                .name("VidioAiAgent")
                                .email("contact@vidioaiagent.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Dev")
                ));
    }
}
