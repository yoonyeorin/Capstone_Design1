package com.example.WayGo.Entity.enums;

import lombok.Getter;

/**
 * 일정 상태
 *
 * 상태 전이:
 * ACTIVE (진행 중) → COMPLETED (완료) or CANCELLED (취소)
 *
 * ACTIVE: 여행 전 또는 여행 중
 * COMPLETED: 여행 완료
 * CANCELLED: 일정 취소
 */
@Getter
public enum ItineraryStatus {
    ACTIVE("진행중"),
    COMPLETED("완료"),
    CANCELLED("취소됨");

    private final String displayName;

    ItineraryStatus(String displayName) {
        this.displayName = displayName;
    }
}