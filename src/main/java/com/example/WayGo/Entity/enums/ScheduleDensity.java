package com.example.WayGo.Entity.enums;

import lombok.Getter;

/**
 * 일정 밀도 (하루 일정의 강도)
 *
 * RELAXED (느슨):
 * - 시작: 11:00
 * - 장소: 2개
 * - 점심: 1회 (고정)
 * - 저녁: 1회 (마지막 장소 근처)
 * - 총 일정: 약 7~8시간
 *
 * PACKED (빡빡):
 * - 시작: 08:00
 * - 장소: 4개
 * - 점심: 1회 (고정)
 * - 저녁: 1회
 * - 총 일정: 약 11~12시간
 *
 * 체류시간 차이:
 * - RELAXED: 장소당 2~3시간 (여유)
 * - PACKED: 장소당 1~1.5시간 (빠르게)
 */
@Getter
public enum ScheduleDensity {
    RELAXED("느슨하게", 11, 2),
    PACKED("빡빡하게", 8, 4);

    private final String displayName;
    private final int startHour;        // 일정 시작 시간 (시)
    private final int placesPerDay;     // 하루 방문 장소 개수

    ScheduleDensity(String displayName, int startHour, int placesPerDay) {
        this.displayName = displayName;
        this.startHour = startHour;
        this.placesPerDay = placesPerDay;
    }
}