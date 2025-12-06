package com.cover.time2gather.domain.meeting;

/**
 * 모임 시간 선택 타입
 */
public enum SelectionType {
    /**
     * 시간 단위 선택 (기존 방식)
     * 날짜별로 특정 시간대들을 선택
     * 예: {"2024-02-15": [9, 10, 11]} -> 9시, 10시, 11시
     */
    TIME,

    /**
     * 일 단위 선택 (신규)
     * 날짜만 선택하고 하루 종일 가능
     * 예: {"2024-02-15": []} -> 2024-02-15 하루 종일
     */
    ALL_DAY
}

