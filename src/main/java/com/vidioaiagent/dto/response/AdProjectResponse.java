package com.vidioaiagent.dto.response;

import com.vidioaiagent.enums.Platform;
import com.vidioaiagent.enums.ProjectStatus;
import com.vidioaiagent.enums.Style;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdProjectResponse {

    private Long id;
    private String productName;
    private String productDescription;
    private String targetAudience;
    private Platform platform;
    private Style adStyle;
    private ProjectStatus status;
    private Integer progressPercent;
    private String trendAnalysis;
    private String adCopy;
    private String script;
    private String hashtags;
    private String hookText;
    private String subtitles;
    private String videoUrl;
    private String thumbnailUrl;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
