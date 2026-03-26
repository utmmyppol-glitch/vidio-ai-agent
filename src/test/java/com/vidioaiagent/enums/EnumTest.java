package com.vidioaiagent.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnumTest {

    @Test
    @DisplayName("Platform enum - 프론트와 값 일치 확인")
    void platform_values_match_frontend() {
        assertThat(Platform.values()).hasSize(4);
        assertThat(Platform.valueOf("YOUTUBE_SHORTS").getDisplayName()).isEqualTo("유튜브 쇼츠");
        assertThat(Platform.valueOf("INSTAGRAM_REELS").getDisplayName()).isEqualTo("인스타그램 릴스");
        assertThat(Platform.valueOf("TIKTOK").getDisplayName()).isEqualTo("틱톡");
        assertThat(Platform.valueOf("YOUTUBE_LONG").getDisplayName()).isEqualTo("유튜브 일반");
    }

    @Test
    @DisplayName("Style enum - 프론트와 값 일치 확인")
    void style_values_match_frontend() {
        assertThat(Style.values()).hasSize(6);
        assertThat(Style.valueOf("EMOTIONAL")).isNotNull();
        assertThat(Style.valueOf("PROVOCATIVE")).isNotNull();
        assertThat(Style.valueOf("INFORMATIVE")).isNotNull();
        assertThat(Style.valueOf("HUMOROUS")).isNotNull();
        assertThat(Style.valueOf("LUXURY")).isNotNull();
        assertThat(Style.valueOf("MINIMAL")).isNotNull();
    }

    @Test
    @DisplayName("ProjectStatus enum - 프론트와 값 일치 확인")
    void projectStatus_values_match_frontend() {
        assertThat(ProjectStatus.values()).hasSize(8);
        assertThat(ProjectStatus.valueOf("PENDING")).isNotNull();
        assertThat(ProjectStatus.valueOf("TREND_ANALYZING")).isNotNull();
        assertThat(ProjectStatus.valueOf("COPY_GENERATING")).isNotNull();
        assertThat(ProjectStatus.valueOf("SCRIPT_GENERATING")).isNotNull();
        assertThat(ProjectStatus.valueOf("VIDEO_GENERATING")).isNotNull();
        assertThat(ProjectStatus.valueOf("THUMBNAIL_GENERATING")).isNotNull();
        assertThat(ProjectStatus.valueOf("COMPLETED")).isNotNull();
        assertThat(ProjectStatus.valueOf("FAILED")).isNotNull();
    }

    @Test
    @DisplayName("Platform - 영상 길이 제한 확인")
    void platform_maxDuration() {
        assertThat(Platform.YOUTUBE_SHORTS.getMaxDurationSeconds()).isEqualTo(60);
        assertThat(Platform.INSTAGRAM_REELS.getMaxDurationSeconds()).isEqualTo(90);
        assertThat(Platform.TIKTOK.getMaxDurationSeconds()).isEqualTo(60);
        assertThat(Platform.YOUTUBE_LONG.getMaxDurationSeconds()).isEqualTo(600);
    }
}
