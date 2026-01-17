package com.cover.time2gather.api.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "미팅 일정 확정 요청")
public class ConfirmMeetingRequest {

    @NotNull(message = "날짜는 필수입니다")
    @Schema(description = "확정할 날짜", example = "2024-02-15", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate date;

    @Schema(description = "확정할 슬롯 인덱스 (TIME 타입인 경우 필수, ALL_DAY 타입인 경우 null)", example = "28")
    private Integer slotIndex;
}
