package com.cover.time2gather.api.meeting.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "장소 추가 요청")
public class AddLocationRequest {

    @NotBlank(message = "장소 이름은 필수입니다.")
    @Schema(description = "장소 이름", example = "강남역 스타벅스")
    private String name;
}
