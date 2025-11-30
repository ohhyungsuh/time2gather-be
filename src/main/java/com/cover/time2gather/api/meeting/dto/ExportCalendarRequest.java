package com.cover.time2gather.api.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캘린더 export 요청")
public class ExportCalendarRequest {

    @NotBlank(message = "날짜는 필수입니다")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "날짜 형식은 yyyy-MM-dd 입니다")
    @Schema(description = "일정 날짜", example = "2024-02-15", requiredMode = Schema.RequiredMode.REQUIRED)
    private String date;

    @NotBlank(message = "시간은 필수입니다")
    @Pattern(regexp = "\\d{2}:\\d{2}", message = "시간 형식은 HH:mm 입니다")
    @Schema(description = "일정 시간", example = "14:30", requiredMode = Schema.RequiredMode.REQUIRED)
    private String time;
}

