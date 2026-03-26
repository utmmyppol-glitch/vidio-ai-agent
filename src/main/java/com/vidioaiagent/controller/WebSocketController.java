package com.vidioaiagent.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/subscribe/{projectId}")
    public void subscribeToProject(@DestinationVariable Long projectId) {
        log.info("WebSocket 구독 요청 - 프로젝트 ID: {}", projectId);
        Map<String, Object> payload = Map.of("step", "SUBSCRIBED", "message", "프로젝트 진행상황 구독이 시작되었습니다");
        messagingTemplate.convertAndSend("/topic/progress/" + projectId, (Object) payload);
    }
}
