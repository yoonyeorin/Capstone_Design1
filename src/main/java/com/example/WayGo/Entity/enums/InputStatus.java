package com.example.WayGo.Entity.enums;

/**
 * 일정 입력 상태
 *
 * 상태 전이 다이어그램:
 *
 * [생성] → IN_PROGRESS
 *             ↓ (Step 8 완료)
 *         COMPLETED
 *             ↓ (일정 생성)
 *         GENERATED
 *
 * 각 상태별 가능한 작업:
 * - IN_PROGRESS: Step 입력, 수정, 삭제 가능
 * - COMPLETED: 일정 생성 가능, 입력 수정 가능
 * - GENERATED: 조회만 가능 (수정 불가)
 */
public enum InputStatus {
    /**
     * 입력 진행 중
     * - Step 1~7 중 하나라도 미완성
     */
    IN_PROGRESS,

    /**
     * 입력 완료
     * - Step 1~8 모두 완료
     * - 일정 생성 가능한 상태
     */
    COMPLETED,

    /**
     * 일정 생성 완료
     * - 이미 Itinerary(일정)이 생성됨
     * - 더 이상 수정 불가 (Read-Only)
     */
    GENERATED
}