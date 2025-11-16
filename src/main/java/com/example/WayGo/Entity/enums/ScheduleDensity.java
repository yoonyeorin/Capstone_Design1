package com.example.WayGo.Entity.enums;

public enum ScheduleDensity {
    RELAXED(11, 2),   // 11시 시작, 하루 2곳
    PACKED(8, 4);     // 8시 시작, 하루 4곳

    private final int startHour;
    private final int targetPlacesPerDay;

    ScheduleDensity(int startHour, int targetPlacesPerDay) {
        this.startHour = startHour;
        this.targetPlacesPerDay = targetPlacesPerDay;
    }

    public int getStartHour() {
        return startHour;
    }

    public int getTargetPlacesPerDay() {
        return targetPlacesPerDay;
    }
}
