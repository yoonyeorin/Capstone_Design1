package com.example.WayGo.Entity.enums;

/**
 * 일정 상태 Enum
 * - 일정 생성 / 사용 / 보관 상태 관리
 */
public enum ItineraryStatus {

    GENERATED,  // 일정 생성됨 (AI 생성 직후)
    ACTIVE,     // 사용 중인 일정
    ARCHIVED,   // 지난 일정
    //DRAFT       // 임시 저장
}
