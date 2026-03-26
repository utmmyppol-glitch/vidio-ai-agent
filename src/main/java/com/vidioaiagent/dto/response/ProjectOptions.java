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
public class ProjectOptions {

    private List<PlatformOption> platforms;
    private List<AdStyleOption> adStyles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlatformOption {
        private String value;
        private String label;
        private String aspectRatio;
        private int maxDuration;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdStyleOption {
        private String value;
        private String label;
        private String description;
    }
}
