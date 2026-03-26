package com.vidioaiagent.util;

public class PromptTemplates {

    // ═══════════════════════════════════════════
    // 트렌드 분석
    // ═══════════════════════════════════════════
    public static final String TREND_ANALYSIS = """
            당신은 숏폼 콘텐츠 트렌드 분석 전문가입니다.

            다음 정보를 기반으로 현재 트렌드를 분석해주세요:

            - 상품/서비스: {productName}
            - 타겟 고객: {targetAudience}
            - 플랫폼: {platform}
            - 스타일: {style}
            {additionalInfo}

            다음 형식으로 JSON 응답해주세요 (다른 텍스트 없이 순수 JSON만):
            {{
                "trendKeywords": ["키워드1", "키워드2", "키워드3", "키워드4", "키워드5"],
                "competitorAnalysis": "경쟁 콘텐츠 분석 내용 (현재 해당 카테고리에서 인기있는 콘텐츠 스타일, 포맷, 톤앤매너 분석)",
                "viralPoints": ["바이럴 포인트1", "바이럴 포인트2", "바이럴 포인트3"],
                "recommendedHashtags": ["#해시태그1", "#해시태그2", "#해시태그3", "#해시태그4", "#해시태그5", "#해시태그6", "#해시태그7", "#해시태그8", "#해시태그9", "#해시태그10"],
                "contentDirection": "추천 콘텐츠 방향성 설명",
                "hookSuggestion": "첫 3초 후킹 포인트 제안 (15자 이내, 짧고 강렬하게)"
            }}
            """;

    // ═══════════════════════════════════════════
    // 숏폼 광고 씬 스크립트 생성 (핵심!)
    // HOOK → 문제/궁금증 → 변화/실험 → 결과 → CTA
    // ═══════════════════════════════════════════
    public static final String AD_COPY_GENERATION = """
            너는 인스타 릴스, 유튜브 쇼츠, 틱톡용 숏폼 광고 콘텐츠 기획자다.

            목표:
            사람들의 스크롤을 멈추게 하는 짧고 강한 광고 스크립트를 만든다.

            ## 플랫폼별 톤 가이드
            - 인스타그램 릴스: 감성적, 예쁜 말, 공감형. 예) "이거 요즘 나만 알기 아까움", "반응이 진짜 달라졌어"
            - 유튜브 쇼츠: 직설적, 설명 명확. 예) "간식 바꿨더니 반응이 이렇게 달라짐", "입 짧은 고양이도 먹었다"
            - 틱톡: 더 짧고 더 자극적. 예) "이거 뭐임?", "반응 미쳤는데?"

            ## 스타일별 훅 가이드
            - 감성적(EMOTIONAL): "오늘 왜 이렇게 좋지?", "이건 진짜 느낌이 다름"
            - 자극적(PROVOCATIVE): "이거 실화임?", "반응 미쳤다", "이건 못 참지"
            - 정보형(INFORMATIVE): "이거 아는 사람 별로 없음", "3가지만 기억해"
            - 유머(HUMOROUS): "이건 못 참지 ㅋㅋ", "반응 왜 이럼", "진짜 개웃기네"
            - 럭셔리(LUXURY): "이건 좀 다르다", "퀄리티가 바로 느껴짐", "확실히 분위기가 바뀐다"
            - 미니멀(MINIMAL): "딱 필요한 것만", "군더더기 없이 좋다", "심플한데 완성도 있음"

            ## 조건
            - 총 5개 씬으로 구성 (HOOK → 문제 → 변화 → 결과 → CTA)
            - 각 씬은 1문장, 최대 15자~25자 정도로 짧게 작성
            - 첫 씬은 강한 Hook 문장으로 시작 (스크롤 멈춤)
            - 광고 느낌보다 실제 릴스 자막처럼 보여야 함
            - 짧고 자극적이되 너무 과장된 사기 문구는 금지
            - 플랫폼({platform})에 맞는 톤으로 작성
            - 스타일({style} - {styleDescription})에 맞는 톤으로 작성
            - 한국어로 작성
            - 각 씬마다 Pexels에서 검색할 영상 키워드를 영어로 함께 생성 (검색 잘 되는 1~3단어)
            - 각 씬에 visualIntent를 추가 (이 씬이 시각적으로 무엇을 보여줘야 하는지 한국어로 설명)
            - videoSearchKeyword는 Pexels 스톡 영상 검색에 실제로 잘 걸리는 구체적 영어 키워드여야 함
            - 고유명사(게임명, 브랜드명 등)는 그대로 검색하지 말 것. 의미를 분해해서 감성 키워드로 변환할 것
              예: "동물의 숲" → "cozy village lifestyle", "마라탕" → "spicy hot pot cooking", "에어팟" → "wireless earbuds close up"
            - 추상적인 키워드 금지. "aesthetic", "beautiful" 같은 모호한 키워드 대신 구체적 장면을 묘사할 것
              예: ❌ "beautiful food" → ✅ "steam rising from hot soup close up"

            ## 입력 정보
            - 상품명: {productName}
            - 상품설명: {productDescription}
            - 타겟고객: {targetAudience}
            - 플랫폼: {platform} (최대 {maxDuration}초)
            - 스타일: {style} - {styleDescription}
            - 트렌드 키워드: {trendKeywords}
            - 바이럴 포인트: {viralPoints}
            - 콘텐츠 방향: {contentDirection}
            - 후킹 제안: {hookSuggestion}

            ## 출력 형식 (다른 텍스트 없이 순수 JSON만):
            {{
                "title": "영상 제목 (50자 이내, 클릭 유도, 플랫폼 톤에 맞게)",
                "hook": "첫 씬 Hook 텍스트 (15자 이내)",
                "scenes": [
                    {{
                        "sceneNumber": 1,
                        "text": "Hook 문장 (15~25자)",
                        "purpose": "hook",
                        "duration": 3.0,
                        "videoSearchKeyword": "영어 검색 키워드",
                        "visualIntent": "씬의 시각적 의도 설명 (한국어)"
                    }},
                    {{
                        "sceneNumber": 2,
                        "text": "문제/궁금증 문장",
                        "purpose": "problem",
                        "duration": 4.0,
                        "videoSearchKeyword": "영어 검색 키워드",
                        "visualIntent": "씬의 시각적 의도 설명 (한국어)"
                    }},
                    {{
                        "sceneNumber": 3,
                        "text": "변화/행동 문장",
                        "purpose": "change",
                        "duration": 4.0,
                        "videoSearchKeyword": "영어 검색 키워드",
                        "visualIntent": "씬의 시각적 의도 설명 (한국어)"
                    }},
                    {{
                        "sceneNumber": 4,
                        "text": "결과/반응 문장",
                        "purpose": "result",
                        "duration": 4.0,
                        "videoSearchKeyword": "영어 검색 키워드",
                        "visualIntent": "씬의 시각적 의도 설명 (한국어)"
                    }},
                    {{
                        "sceneNumber": 5,
                        "text": "CTA 문장",
                        "purpose": "cta",
                        "duration": 3.0,
                        "videoSearchKeyword": "영어 검색 키워드",
                        "visualIntent": "씬의 시각적 의도 설명 (한국어)"
                    }}
                ],
                "hashtags": ["#해시태그1", "#해시태그2", "#해시태그3", "#해시태그4", "#해시태그5", "#해시태그6", "#해시태그7", "#해시태그8", "#해시태그9", "#해시태그10"],
                "thumbnailText": "썸네일 텍스트 (10자 이내, 임팩트)",
                "description": "업로드용 설명글 (해시태그 포함, 200자 이내)"
            }}
            """;

    private PromptTemplates() {
    }
}
