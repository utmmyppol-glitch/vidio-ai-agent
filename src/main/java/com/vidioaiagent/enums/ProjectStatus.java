package com.vidioaiagent.enums;

public enum ProjectStatus {
    PENDING("대기중"),
    TREND_ANALYZING("트렌드 분석중"),
    COPY_GENERATING("카피 생성중"),
    SCRIPT_GENERATING("스크립트 생성중"),
    VIDEO_GENERATING("영상 생성중"),
    THUMBNAIL_GENERATING("썸네일 생성중"),
    COMPLETED("완료"),
    FAILED("실패");

    private final String displayName;

    ProjectStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
