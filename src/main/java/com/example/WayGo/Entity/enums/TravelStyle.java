package com.example.WayGo.Entity.enums;

public enum TravelStyle {
    FOOD("맛집"),
    CULTURE("박물관 관광지"),
    NATURE("자연 관광지"),
    SHOPPING("쇼핑 거리"),
    ACTIVE("액티비티"),
    RELAXATION("휴양 여행지");

    private final String placesKeyword;

    TravelStyle(String placesKeyword) {
        this.placesKeyword = placesKeyword;
    }

    public String getPlacesKeyword() {
        return placesKeyword;
    }
}
