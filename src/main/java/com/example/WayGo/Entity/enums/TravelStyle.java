package com.example.WayGo.Entity.enums;

import lombok.Getter;

/**
 * 여행 취향 (최대 2개 선택)
 *
 * 알고리즘 연동:
 * 1. Google Places API 검색 시 type 필터링
 * 2. 평점 가중치 조정
 * 3. 장소 우선순위 결정
 *
 * 예시 매칭:
 * ACTIVE → type: ["hiking", "park", "amusement_park"]
 * NATURE → type: ["park", "natural_feature", "campground"]
 * CULTURE → type: ["museum", "art_gallery", "historical"]
 * FOOD → type: ["restaurant", "cafe", "food"]
 * CITY → type: ["shopping_mall", "night_club", "tourist_attraction"]
 * RELAXED → type: ["spa", "beach", "cafe"]
 */
@Getter
public enum TravelStyle {
    ACTIVE("활동적인 스타일"),
    RELAXED("차분한 휴양 스타일"),
    NATURE("자연 탐방 스타일"),
    CULTURE("문화/역사 탐방 스타일"),
    FOOD("미식 여행 스타일"),
    CITY("도시 탐험 스타일");

    private final String displayName;

    TravelStyle(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Google Places API type 매핑
     * 나중에 알고리즘 브랜치에서 사용
     */
    public String[] getPlaceTypes() {
        return switch (this) {
            case ACTIVE -> new String[]{"amusement_park", "park", "stadium"};
            case RELAXED -> new String[]{"spa", "beach", "cafe"};
            case NATURE -> new String[]{"park", "natural_feature", "campground"};
            case CULTURE -> new String[]{"museum", "art_gallery", "historical"};
            case FOOD -> new String[]{"restaurant", "cafe", "food"};
            case CITY -> new String[]{"shopping_mall", "tourist_attraction", "night_club"};
        };
    }
}