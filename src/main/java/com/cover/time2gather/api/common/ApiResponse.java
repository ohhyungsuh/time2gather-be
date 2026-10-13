package com.cover.time2gather.api.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Schema(description = "공통 API 응답")
@Getter
@RequiredArgsConstructor
public class ApiResponse<T> {

    @Schema(description = "성공 여부", example = "true")
    private final boolean success;

    @Schema(description = "응답 데이터")
    private final T data;

    @Schema(description = "에러 메시지 (실패 시)", example = "Invalid authorization code")
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "클라이언트 처리를 위한 에러 메시지 키", example = "error.meeting.not.found")
    private final String messageKey;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, null);
    }

    public static <T> ApiResponse<T> error(String message, String messageKey) {
        return new ApiResponse<>(false, null, message, messageKey);
    }
}
