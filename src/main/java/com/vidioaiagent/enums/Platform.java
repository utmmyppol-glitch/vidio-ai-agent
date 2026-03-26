package com.vidioaiagent.enums;

public enum Platform {
    YOUTUBE_SHORTS("유튜브 쇼츠", "9:16", 60),
    INSTAGRAM_REELS("인스타그램 릴스", "9:16", 90),
    TIKTOK("틱톡", "9:16", 60),
    YOUTUBE_LONG("유튜브 일반", "16:9", 600);

    private final String displayName;
    private final String aspectRatio;
    private final int maxDurationSeconds;

    Platform(String displayName, String aspectRatio, int maxDurationSeconds) {
        this.displayName = displayName;
        this.aspectRatio = aspectRatio;
        this.maxDurationSeconds = maxDurationSeconds;
    }

    public String getDisplayName() { return displayName; }
    public String getAspectRatio() { return aspectRatio; }
    public int getMaxDurationSeconds() { return maxDurationSeconds; }
}
