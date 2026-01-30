package com.cover.time2gather.api.meeting.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Add location request")
public class AddLocationRequest {

    @NotBlank(message = "{validation.location.name.required}")
    @Schema(description = "Location name", example = "Gangnam Station Starbucks")
    private String name;
}
