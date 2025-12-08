package com.cover.time2gather.api.meeting.dto.request;

import com.cover.time2gather.domain.meeting.vo.TimeSlot;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
        ## 사용자 시간 선택 요청
        
        ### 📌 중요: 각 날짜마다 type 필드를 반드시 명시해야 합니다!
        
        ### 🎯 type 필드 값
        - **"TIME"**: 시간 단위 선택 (특정 시간대를 선택)
        - **"ALL_DAY"**: 일 단위 선택 (하루 종일 가능)
        
        ### 📋 필드 설명
        
        #### 1. selections (배열, 필수)
        - 선택한 날짜들의 배열
        - 비어있으면 안됨 (최소 1개 날짜 필요)
        
        #### 2. selections[].date (문자열, 필수)
        - 형식: YYYY-MM-DD
        - 예: "2024-12-15"
        
        #### 3. selections[].type (문자열, 필수)
        - 값: "TIME" 또는 "ALL_DAY"
        - 대소문자 구분 없음
        
        #### 4. selections[].times (배열)
        - **type="TIME"인 경우**: 필수! 최소 1개 시간 필요
        - **type="ALL_DAY"인 경우**: 무시됨 (null, [], 뭘 보내든 무시)
        - 형식: "HH:mm" (24시간 형식)
        - 예: ["09:00", "10:00", "11:00"]
        
        ---
        
        ### ✅ 올바른 사용 예시
        
        #### 예시 1: 시간 단위 선택 (TIME)
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
        
        #### 예시 2: 일 단위 선택 (ALL_DAY)
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
        
        #### 예시 3: 혼합 (모임이 TIME 타입인 경우만 가능)
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
        **참고**: 선택하지 않은 날짜는 배열에서 제외
        
        ---
        
        ### ❌ 잘못된 사용 예시
        
        #### 에러 1: TYPE인데 times가 비어있음
        ```json
        {
          "selections": [
            {
              "date": "2024-12-15",
              "type": "TIME",
              "times": []  // ❌ 에러! TIME은 times 필수
            }
          ]
        }
        ```
        **에러 메시지**: "날짜 '2024-12-15'는 TIME 타입인데 시간이 지정되지 않았습니다."
        
        #### 에러 2: TYPE인데 times가 null
        ```json
        {
          "selections": [
            {
              "date": "2024-12-15",
              "type": "TIME",
              "times": null  // ❌ 에러! TIME은 times 필수
            }
          ]
        }
        ```
        **에러 메시지**: "날짜 '2024-12-15'는 TIME 타입인데 시간이 지정되지 않았습니다."
        
        #### 에러 3: 잘못된 타입
        ```json
        {
          "selections": [
            {
              "date": "2024-12-15",
              "type": "FULL_DAY",  // ❌ 에러! TIME 또는 ALL_DAY만 가능
              "times": ["09:00"]
            }
          ]
        }
        ```
        **에러 메시지**: "알 수 없는 타입: FULL_DAY (TIME 또는 ALL_DAY만 가능)"
        
        #### 에러 4: type 필드 누락
        ```json
        {
          "selections": [
            {
              "date": "2024-12-15",
              "times": ["09:00"]  // ❌ 에러! type 필수
            }
          ]
        }
        ```
        **에러 메시지**: "타입은 필수입니다"
        
        ---
        
        ### 🔍 빈 배열 vs null 의미
        
        #### times 필드가 빈 배열 [] 인 경우
        - **type="TIME"**: ❌ 에러 발생
        - **type="ALL_DAY"**: ✅ 정상 (무시됨)
        
        #### times 필드가 null 인 경우
        - **type="TIME"**: ❌ 에러 발생
        - **type="ALL_DAY"**: ✅ 정상 (무시됨)
        
        #### times 필드가 아예 없는 경우
        - **type="TIME"**: ❌ 에러 발생
        - **type="ALL_DAY"**: ✅ 정상 (무시됨)
        
        #### 날짜를 선택하지 않는 경우
        - 해당 날짜를 selections 배열에서 **제외**하면 됨
        - null이나 빈 객체를 보내지 마세요
        
        ---
        
        ### 💡 팁
        
        1. **먼저 모임 정보를 조회**하여 selectionType을 확인하세요
           - `GET /api/v1/meetings/{code}`
           - response.meeting.selectionType 확인
        
        2. **모임 타입에 맞게 요청 구성**
           - selectionType="TIME" → type="TIME" 사용
           - selectionType="ALL_DAY" → type="ALL_DAY" 사용
        
        3. **혼합 불가**
           - ALL_DAY 모임에 TIME 타입 선택 → 서버 에러
           - TIME 모임에 ALL_DAY 타입 선택 → 서버 에러
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

    @NotNull(message = "선택한 시간대는 필수입니다")
    @Schema(
        description = """
            날짜별 선택 정보 배열
            
            - 최소 1개 이상의 날짜 필요
            - 각 날짜마다 date, type 필드 필수
            - type="TIME"인 경우 times 필드도 필수
            """,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<DateSelection> selections;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(
        description = """
            날짜별 선택 정보
            
            각 날짜에 대한 선택 타입과 시간을 지정합니다.
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

        @NotNull(message = "날짜는 필수입니다")
        @Schema(
            description = """
                선택한 날짜 (YYYY-MM-DD 형식)
                
                - 형식: YYYY-MM-DD
                - 예시: "2024-12-15", "2024-01-01"
                - 필수 입력
                
                ⚠️ 주의:
                - 모임의 availableDates에 포함된 날짜만 선택 가능
                - 잘못된 날짜 형식 시 에러 발생
                """,
            example = "2024-12-15",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String date;

        @NotNull(message = "타입은 필수입니다")
        @Schema(
            description = """
                선택 타입 (TIME 또는 ALL_DAY)
                
                ### 가능한 값:
                1. **"TIME"**: 시간 단위 선택
                   - 특정 시간대를 선택하는 경우
                   - times 필드 필수! (최소 1개 시간 필요)
                   - 예: ["09:00", "10:00", "11:00"]
                
                2. **"ALL_DAY"**: 일 단위 선택
                   - 하루 종일 가능한 경우
                   - times 필드 무시됨 (null, [], 뭐든 상관없음)
                
                ### 주의사항:
                - 대소문자 구분 없음 ("time", "Time", "TIME" 모두 가능)
                - 필수 입력
                - TIME도 ALL_DAY도 아닌 값 입력 시 에러
                
                ### 모임 타입과의 관계:
                - 모임이 TIME 타입 → type="TIME" 사용
                - 모임이 ALL_DAY 타입 → type="ALL_DAY" 사용
                - 불일치 시 서버에서 에러 발생
                
                💡 팁: GET /meetings/{code}로 모임 정보를 먼저 확인하세요!
                """,
            allowableValues = {"TIME", "ALL_DAY"},
            example = "TIME",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String type;

        @Schema(
            description = """
                선택한 시간대 배열 (HH:mm 형식)
                
                ### type="TIME"인 경우:
                - **필수 입력!**
                - 최소 1개 이상의 시간 필요
                - 형식: "HH:mm" (24시간 형식)
                - 예시: ["09:00", "10:00", "11:00", "14:30"]
                - ❌ null, [] (빈 배열) → 에러 발생!
                
                ### type="ALL_DAY"인 경우:
                - **무시됨** (아무 값이나 보내도 됨)
                - null 가능
                - [] (빈 배열) 가능
                - 시간 값 있어도 무시됨
                - 권장: 필드 자체를 생략하거나 null 전송
                
                ### 시간 형식:
                - "00:00" ~ "23:00" (정시만 가능, 기본 60분 간격)
                - 잘못된 형식: "9:00", "09:0", "25:00" → 에러
                - 올바른 형식: "09:00", "14:00", "23:00"
                
                ### 예시:
                
                #### ✅ 올바른 예시 (TIME):
                ```json
                {
                  "date": "2024-12-15",
                  "type": "TIME",
                  "times": ["09:00", "10:00", "11:00"]
                }
                ```
                
                #### ✅ 올바른 예시 (ALL_DAY):
                ```json
                {
                  "date": "2024-12-15",
                  "type": "ALL_DAY"
                }
                ```
                또는
                ```json
                {
                  "date": "2024-12-15",
                  "type": "ALL_DAY",
                  "times": null
                }
                ```
                또는
                ```json
                {
                  "date": "2024-12-15",
                  "type": "ALL_DAY",
                  "times": []
                }
                ```
                
                #### ❌ 잘못된 예시 (TIME에 빈 배열):
                ```json
                {
                  "date": "2024-12-15",
                  "type": "TIME",
                  "times": []  // ❌ 에러!
                }
                ```
                **에러**: "날짜 '2024-12-15'는 TIME 타입인데 시간이 지정되지 않았습니다."
                
                #### ❌ 잘못된 예시 (TIME에 null):
                ```json
                {
                  "date": "2024-12-15",
                  "type": "TIME",
                  "times": null  // ❌ 에러!
                }
                ```
                **에러**: "날짜 '2024-12-15'는 TIME 타입인데 시간이 지정되지 않았습니다."
                
                ### 🔍 빈 배열 vs null 차이:
                - **type="TIME"**: 둘 다 에러 (시간 필수!)
                - **type="ALL_DAY"**: 둘 다 정상 (무시됨)
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
            throw new IllegalArgumentException("선택 데이터가 없습니다");
        }

        Map<String, int[]> result = new HashMap<>();

        for (DateSelection selection : selections) {
            String date = selection.getDate();
            String type = selection.getType();
            List<String> times = selection.getTimes();

            if ("ALL_DAY".equalsIgnoreCase(type)) {
                // ALL_DAY: 빈 배열
                result.put(date, new int[0]);

            } else if ("TIME".equalsIgnoreCase(type)) {
                // TIME: 시간 배열 필수
                if (times == null || times.isEmpty()) {
                    throw new IllegalArgumentException(
                        String.format("날짜 '%s'는 TIME 타입인데 시간이 지정되지 않았습니다.", date)
                    );
                }

                try {
                    int[] slots = times.stream()
                            .mapToInt(timeStr -> {
                                if (timeStr == null || timeStr.trim().isEmpty()) {
                                    throw new IllegalArgumentException("시간 값이 비어있습니다");
                                }
                                // 모임의 intervalMinutes를 사용하여 변환
                                return TimeSlot.fromTimeString(timeStr.trim(), intervalMinutes).getSlotIndex();
                            })
                            .toArray();
                    result.put(date, slots);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                        String.format("날짜 '%s'의 시간 형식이 올바르지 않습니다. %s", date, e.getMessage())
                    );
                }

            } else {
                throw new IllegalArgumentException(
                    String.format("알 수 없는 타입: %s (TIME 또는 ALL_DAY만 가능)", type)
                );
            }
        }

        return result;
    }
}

