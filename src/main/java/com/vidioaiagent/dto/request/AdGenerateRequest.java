package com.vidioaiagent.dto.request;

import com.vidioaiagent.enums.Platform;
import com.vidioaiagent.enums.Style;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdGenerateRequest {

    @NotBlank(message = "상품명은 필수입니다")
    private String productName;

    private String productDescription;

    @NotBlank(message = "타겟 고객은 필수입니다")
    private String targetAudience;

    @NotNull(message = "플랫폼 선택은 필수입니다")
    private Platform platform;

    @NotNull(message = "스타일 선택은 필수입니다")
    @JsonAlias("adStyle")
    private Style adStyle;

    @JsonAlias("additionalInfo")
    private String additionalRequest;
}
