package com.cover.time2gather.api.meeting.dto.request;
import com.cover.time2gather.domain.meeting.MeetingCandidates;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Getter @NoArgsConstructor @AllArgsConstructor
public class UpdateCandidatesRequest {
    @NotNull @Size(min=1,max=31) private List<@NotNull String> dates;
    @Size(min=1,max=1440) private List<@NotNull @Pattern(regexp="(?:[01][0-9]|2[0-3]):[0-5][0-9]",message="{validation.time.format}") String> times;
    @Size(min=2,max=5) private List<@NotNull @Valid Location> locations;
    @NotBlank @Pattern(regexp="[a-f0-9]{64}") private String expectedVersion;
    @Getter @NoArgsConstructor @AllArgsConstructor public static class Location {
        private Long id;
        @NotBlank @Size(max=200) private String name;
    }
    public MeetingCandidates.Edit edit() {
        return new MeetingCandidates.Edit(dates,times,locations==null?null:locations.stream().map(p->new MeetingCandidates.Location(p.id,p.name)).toList(),expectedVersion);
    }
}
