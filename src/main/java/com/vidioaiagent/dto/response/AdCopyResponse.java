package com.vidioaiagent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdCopyResponse {

    private String title;
    private String hook;
    private List<SceneEntry> scenes;
    private List<String> hashtags;
    private String thumbnailText;
    private String description;

    // 하위 호환용 (레거시)
    private String script;
    private List<SubtitleEntry> subtitles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SceneEntry {
        private int sceneNumber;
        private String text;
        private String purpose;    // hook, problem, change, result, cta
        private double duration;
        private String videoSearchKeyword;  // Pexels 검색용 영어 키워드
        private String visualIntent;        // 씬의 시각적 의도 (예: "긴장감 있는 클로즈업")
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubtitleEntry {
        private double startTime;
        private double endTime;
        private String text;
    }

    /**
     * scenes → subtitles 변환 (하위 호환)
     */
    public List<SubtitleEntry> toSubtitles() {
        if (scenes == null || scenes.isEmpty()) return subtitles;

        List<SubtitleEntry> result = new java.util.ArrayList<>();
        double currentTime = 0;
        for (SceneEntry scene : scenes) {
            result.add(SubtitleEntry.builder()
                    .startTime(currentTime)
                    .endTime(currentTime + scene.getDuration())
                    .text(scene.getText())
                    .build());
            currentTime += scene.getDuration();
        }
        return result;
    }

    /**
     * scenes → script 변환 (하위 호환)
     */
    public String toScript() {
        if (scenes == null || scenes.isEmpty()) return script;

        StringBuilder sb = new StringBuilder();
        for (SceneEntry scene : scenes) {
            sb.append(String.format("[Scene %d - %s] (%.0fs)\n",
                    scene.getSceneNumber(), scene.getPurpose(), scene.getDuration()));
            sb.append(scene.getText()).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * Pexels 검색 키워드 목록
     */
    public List<String> getSearchKeywords() {
        if (scenes == null) return List.of();
        return scenes.stream()
                .map(SceneEntry::getVideoSearchKeyword)
                .filter(k -> k != null && !k.isBlank())
                .toList();
    }
}
