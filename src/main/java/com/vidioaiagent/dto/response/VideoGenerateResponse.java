package com.vidioaiagent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoGenerateResponse {

    private String videoUrl;
    private String thumbnailUrl;
    private String uploadText;
    private String status;
}
