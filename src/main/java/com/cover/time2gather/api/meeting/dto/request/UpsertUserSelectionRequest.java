package com.cover.time2gather.api.meeting.dto.request;

import com.cover.time2gather.domain.exception.BusinessException;
import com.cover.time2gather.domain.exception.ErrorCode;
import com.cover.time2gather.domain.meeting.vo.TimeSlot;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = """
        ## User Time Selection Request
        
        ### Important: The type field must be specified for each date!
        
        ### Type field values
        - **"TIME"**: Hourly selection (select specific time slots)
        - **"ALL_DAY"**: Full day selection (available all day)
        
        ### Field descriptions
        
        #### 1. selections (array, required)
        - Array of selected dates
        - Cannot be empty (minimum 1 date required)
        
        #### 2. selections[].date (string, required)
        - Format: YYYY-MM-DD
        - Example: "2024-12-15"
        
        #### 3. selections[].type (string, required)
        - Value: "TIME" or "ALL_DAY"
        - Case insensitive
        
        #### 4. selections[].times (array)
        - **When type="TIME"**: Required! At least 1 time needed
        - **When type="ALL_DAY"**: Ignored (null, [], anything is ignored)
        - Format: "HH:mm" (24-hour format)
        - Example: ["09:00", "10:00", "11:00"]
        
        ---
        
        ### Correct usage examples
        
        #### Example 1: Hourly selection (TIME)
        ```json
        {
          "selections": [
            {
              "date": "2024-12-15",
              "type": "TIME",
              "times": ["09:00", "10:00", "11:00"]
            },
            {
              "date": "2024-12-16",
              "type": "TIME",
              "times": ["14:00", "15:00"]
            }
          ]
        }
        ```
        
        #### Example 2: Full day selection (ALL_DAY)
        ```json
        {
          "selections": [
            {
              "date": "2024-12-20",
              "type": "ALL_DAY"
            },
            {
              "date": "2024-12-21",
              "type": "ALL_DAY"
            }
          ]
        }
        ```
        
        #### Example 3: Mixed (only when meeting is TIME type)
        ```json
        {
          "selections": [
            {
              "date": "2024-12-15",
              "type": "TIME",
              "times": ["09:00", "10:00"]
            }
          ]
        }
        ```
        **Note**: Unselected dates should be excluded from the array
        
        ---
        
        ### Incorrect usage examples
        
        #### Error 1: TYPE but times is empty
        ```json
        {
          "selections": [
            {
              "date": "2024-12-15",
              "type": "TIME",
              "times": []  // Error! times required for TIME
            }
          ]
        }
        ```
        **Error message**: "Date '2024-12-15' is TIME type but no times specified."
        
        #### Error 2: TYPE but times is null
        ```json
        {
          "selections": [
            {
              "date": "2024-12-15",
              "type": "TIME",
              "times": null  // Error! times required for TIME
            }
          ]
        }
        ```
        **Error message**: "Date '2024-12-15' is TIME type but no times specified."
        
        #### Error 3: Invalid type
        ```json
        {
          "selections": [
            {
              "date": "2024-12-15",
              "type": "FULL_DAY",  // Error! Only TIME or ALL_DAY allowed
              "times": ["09:00"]
            }
          ]
        }
        ```
        **Error message**: "Unknown type: FULL_DAY (only TIME or ALL_DAY allowed)"
        
        #### Error 4: type field missing
        ```json
        {
          "selections": [
            {
              "date": "2024-12-15",
              "times": ["09:00"]  // Error! type is required
            }
          ]
        }
        ```
        **Error message**: "Type is required"
        
        ---
        
        ### Empty array vs null meaning
        
        #### When times field is empty array []
        - **type="TIME"**: Error occurs
        - **type="ALL_DAY"**: OK (ignored)
        
        #### When times field is null
        - **type="TIME"**: Error occurs
        - **type="ALL_DAY"**: OK (ignored)
        
        #### When times field is omitted
        - **type="TIME"**: Error occurs
        - **type="ALL_DAY"**: OK (ignored)
        
        #### When not selecting a date
        - **Exclude** that date from the selections array
        - Do not send null or empty object
        
        ---
        
        ### Tips
        
        1. **First check the meeting info** to see the selectionType
           - `GET /api/v1/meetings/{code}`
           - Check response.meeting.selectionType
        
        2. **Compose request according to meeting type**
           - selectionType="TIME" -> use type="TIME"
           - selectionType="ALL_DAY" -> use type="ALL_DAY"
        
        3. **No mixing allowed**
           - ALL_DAY meeting with TIME type selection -> server error
           - TIME meeting with ALL_DAY type selection -> server error
        """,
    example = """
        {
          "selections": [
            {
              "date": "2024-12-15",
              "type": "TIME",
              "times": ["09:00", "10:00", "11:00"]
            },
            {
              "date": "2024-12-16",
              "type": "TIME",
              "times": ["14:00", "15:00"]
            }
          ]
        }
        """
)
public class UpsertUserSelectionRequest {

    @NotNull(message = "{validation.selection.required}")
    @Size(min = 1, max = 31, message = "{validation.selection.date.min.max}")
    @Schema(
        description = """
            Array of date selection info
            
            - At least 1 date required (max 31)
            - date and type fields are required for each date
            - times field is also required when type="TIME"
            """,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<DateSelection> selections;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(
        description = """
            Date selection info
            
            Specify the selection type and times for each date.
            """,
        example = """
            {
              "date": "2024-12-15",
              "type": "TIME",
              "times": ["09:00", "10:00", "11:00"]
            }
            """
    )
    public static class DateSelection {

        @NotNull(message = "{validation.date.required}")
        @Schema(
            description = """
                Selected date (YYYY-MM-DD format)
                
                - Format: YYYY-MM-DD
                - Example: "2024-12-15", "2024-01-01"
                - Required field
                
                Note:
                - Only dates included in meeting's availableDates can be selected
                - Invalid date format will cause an error
                """,
            example = "2024-12-15",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String date;

        @NotNull(message = "{validation.type.required}")
        @Schema(
            description = """
                Selection type (TIME or ALL_DAY)
                
                ### Possible values:
                1. **"TIME"**: Hourly selection
                   - Select specific time slots
                   - times field required! (at least 1 time needed)
                   - Example: ["09:00", "10:00", "11:00"]
                
                2. **"ALL_DAY"**: Full day selection
                   - Available all day
                   - times field ignored (null, [], anything is fine)
                
                ### Notes:
                - Case insensitive ("time", "Time", "TIME" all work)
                - Required field
                - Error if value is neither TIME nor ALL_DAY
                
                ### Relationship with meeting type:
                - Meeting is TIME type -> use type="TIME"
                - Meeting is ALL_DAY type -> use type="ALL_DAY"
                - Mismatch will cause server error
                
                Tip: Check meeting info first with GET /meetings/{code}!
                """,
            allowableValues = {"TIME", "ALL_DAY"},
            example = "TIME",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String type;

        @Schema(
            description = """
                Selected time slots array (HH:mm format)
                
                ### When type="TIME":
                - **Required!**
                - At least 1 time needed
                - Format: "HH:mm" (24-hour format)
                - Example: ["09:00", "10:00", "11:00", "14:30"]
                - null, [] (empty array) -> Error!
                
                ### When type="ALL_DAY":
                - **Ignored** (any value is fine)
                - null is OK
                - [] (empty array) is OK
                - Time values are ignored even if present
                - Recommended: omit the field or send null
                
                ### Time format:
                - "00:00" ~ "23:00" (hourly only, default 60min interval)
                - Invalid format: "9:00", "09:0", "25:00" -> Error
                - Valid format: "09:00", "14:00", "23:00"
                
                ### Examples:
                
                #### Correct example (TIME):
                ```json
                {
                  "date": "2024-12-15",
                  "type": "TIME",
                  "times": ["09:00", "10:00", "11:00"]
                }
                ```
                
                #### Correct example (ALL_DAY):
                ```json
                {
                  "date": "2024-12-15",
                  "type": "ALL_DAY"
                }
                ```
                or
                ```json
                {
                  "date": "2024-12-15",
                  "type": "ALL_DAY",
                  "times": null
                }
                ```
                or
                ```json
                {
                  "date": "2024-12-15",
                  "type": "ALL_DAY",
                  "times": []
                }
                ```
                
                #### Incorrect example (TIME with empty array):
                ```json
                {
                  "date": "2024-12-15",
                  "type": "TIME",
                  "times": []  // Error!
                }
                ```
                **Error**: "Date '2024-12-15' is TIME type but no times specified."
                
                #### Incorrect example (TIME with null):
                ```json
                {
                  "date": "2024-12-15",
                  "type": "TIME",
                  "times": null  // Error!
                }
                ```
                **Error**: "Date '2024-12-15' is TIME type but no times specified."
                
                ### Empty array vs null difference:
                - **type="TIME"**: Both cause error (times required!)
                - **type="ALL_DAY"**: Both are OK (ignored)
                """,
            example = "[\"09:00\", \"10:00\", \"11:00\"]",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        private List<String> times;
    }

    /**
     * API 형식 → 도메인 slotIndex 변환
     * @param intervalMinutes 모임의 시간 간격 (분 단위)
     */
    public Map<String, int[]> toSlotIndexes(int intervalMinutes) {
        if (selections == null || selections.isEmpty()) {
            throw new BusinessException(ErrorCode.SELECTION_EMPTY);
        }

        Map<String, int[]> result = new HashMap<>();

        for (DateSelection selection : selections) {
            if (selection == null) {
                continue;
            }

            String date = selection.getDate();
            String type = selection.getType();
            List<String> times = selection.getTimes();

            // 필수 필드 검증
            if (date == null || date.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.SELECTION_DATE_REQUIRED);
            }
            if (type == null || type.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.SELECTION_TYPE_REQUIRED);
            }

            if ("ALL_DAY".equalsIgnoreCase(type)) {
                // ALL_DAY: 빈 배열
                result.put(date, new int[0]);

            } else if ("TIME".equalsIgnoreCase(type)) {
                // TIME: 시간 배열 필수
                if (times == null || times.isEmpty()) {
                    throw new BusinessException(ErrorCode.SELECTION_SLOT_EMPTY_FOR_TIME);
                }

                try {
                    int[] slots = times.stream()
                            .mapToInt(timeStr -> {
                                if (timeStr == null || timeStr.trim().isEmpty()) {
                                    throw new BusinessException(ErrorCode.SELECTION_TIME_EMPTY);
                                }
                                // 모임의 intervalMinutes를 사용하여 변환
                                return TimeSlot.fromTimeString(timeStr.trim(), intervalMinutes).getSlotIndex();
                            })
                            .toArray();
                    result.put(date, slots);
                } catch (BusinessException e) {
                    throw e;
                } catch (IllegalArgumentException e) {
                    throw new BusinessException(ErrorCode.SELECTION_TIME_FORMAT_INVALID);
                }

            } else {
                throw new BusinessException(ErrorCode.SELECTION_TYPE_UNKNOWN);
            }
        }

        return result;
    }
}

