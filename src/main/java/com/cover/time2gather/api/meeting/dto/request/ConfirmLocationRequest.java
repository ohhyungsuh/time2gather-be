package com.cover.time2gather.api.meeting.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "장소 확정 요청")
public class ConfirmLocationRequest {

    @NotNull(message = "장소 ID는 필수입니다.")
    @Schema(description = "확정할 장소 ID", example = "1")
    private Long locationId;
}
