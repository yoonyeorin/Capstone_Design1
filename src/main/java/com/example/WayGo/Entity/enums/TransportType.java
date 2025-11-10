package com.example.WayGo.Entity.enums;

import lombok.Getter;

/**
 * 교통수단 종류
 *
 * Entity에서만 사용! (DTO에서는 String 사용)
 *
 * 각 교통수단별 특징:
 * - BUS: 저렴, 중단거리(1~10km)
 * - SUBWAY: 빠름, 장거리(10km+), 정확한 시간
 * - TAXI: 편함, 비쌈, 단거리(~5km)
 * - WALK: 무료, 단거리(~1km), 체험형
 * - CAR: 자유로움, 주차 문제, 장거리
 *
 * 알고리즘 사용:
 * - 거리 < 1km → WALK 우선
 * - 1~5km → BUS/SUBWAY 우선
 * - 5km+ → TAXI/CAR/SUBWAY
 * - 같은 수단 3번 연속 → 다른 수단 사용
 */
@Getter
public enum TransportType {
    BUS("버스"),
    SUBWAY("지하철"),
    TAXI("택시"),
    WALK("도보"),
    CAR("자차(렌트)");

    private final String displayName;

    TransportType(String displayName) {
        this.displayName = displayName;
    }
}