package com.example.WayGo.Entity.enums;


import lombok.Getter;

/**
 * 활동 타입
 *
 * PLACE: 관광지 (입장료 있음)
 * MEAL: 식사 (식비 있음)
 * ACCOMMODATION: 숙소 (하루 종료 후 체크인)
 *
 * 사용 예:
 * - 09:30 센소지 방문 → PLACE
 * - 12:00 점심 식사 → MEAL
 * - 20:00 호텔 체크인 → ACCOMMODATION
 */
@Getter
public enum ActivityType {
    PLACE("관광지"),
    MEAL("식사"),
    ACCOMMODATION("숙소");

    private final String displayName;

    ActivityType(String displayName) {
        this.displayName = displayName;
    }
}