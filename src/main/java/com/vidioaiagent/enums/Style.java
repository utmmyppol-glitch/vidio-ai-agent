package com.vidioaiagent.enums;

public enum Style {
    EMOTIONAL("감성적", "감정에 호소하는 따뜻하고 공감가는 톤"),
    PROVOCATIVE("자극적", "강렬하고 임팩트 있는 후킹 중심의 톤"),
    INFORMATIVE("정보형", "팩트 기반의 신뢰감 있는 설명 톤"),
    HUMOROUS("유머", "웃기고 재미있는 밈 스타일의 톤"),
    LUXURY("럭셔리", "고급스럽고 세련된 프리미엄 톤"),
    MINIMAL("미니멀", "깔끔하고 군더더기 없는 심플한 톤");

    private final String displayName;
    private final String description;

    Style(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
