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
public class TrendAnalysisResponse {

    private List<String> trendKeywords;
    private String competitorAnalysis;
    private List<String> viralPoints;
    private List<String> recommendedHashtags;
    private String contentDirection;
    private String hookSuggestion;
}
