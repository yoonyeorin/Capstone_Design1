package com.example.WayGo.Dto.Itinerary;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalTime;

/**
 * Step 3: 교통편 정보
 *
 * API: PUT /api/itinerary/input/{inputId}/step3
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Step3RequestDto {

    @NotNull(message = "승차권 예매 여부를 선택해주세요")
    private Boolean hasTicket;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime arrivalTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime departureTime;

    @AssertTrue(message = "승차권을 예매했다면 도착/출발 시간을 모두 입력해주세요")
    public boolean isValidTimes() {
        if (Boolean.FALSE.equals(hasTicket)) {
            return true;
        }

        if (arrivalTime == null || departureTime == null) {
            return false;
        }

        return departureTime.isAfter(arrivalTime);
    }
}