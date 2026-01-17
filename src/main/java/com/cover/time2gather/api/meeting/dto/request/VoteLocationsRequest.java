package com.cover.time2gather.api.meeting.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "장소 투표 요청")
public class VoteLocationsRequest {

    @NotNull(message = "장소 ID 목록은 필수입니다.")
    @Schema(description = "투표할 장소 ID 목록 (빈 배열이면 투표 스킵)", example = "[1, 2]")
    private List<Long> locationIds;
}
