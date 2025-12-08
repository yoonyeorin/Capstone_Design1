package com.example.WayGo.Service.Itinerary;

import com.example.WayGo.Dto.Itinerary.ItinerarySaveRequestDto;
import com.example.WayGo.Dto.Itinerary.ItinerarySaveResponseDto;
import com.example.WayGo.Entity.Itinerary.Itinerary;
import com.example.WayGo.Entity.enums.ItineraryStatus;
import com.example.WayGo.Repository.Itinerary.ItineraryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItinerarySaveService {

    private final ItineraryRepository itineraryRepository;

    /**
     * 일정 저장 (겹침 체크 + 필요 시 기존 일정 삭제)
     */
    @Transactional
    public ItinerarySaveResponseDto saveItinerary(ItinerarySaveRequestDto req) {

        Itinerary target = itineraryRepository.findById(req.getItineraryId())
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다."));

        if (!target.getUserId().equals(req.getUserId())) {
            throw new RuntimeException("본인 일정만 저장할 수 있습니다.");
        }

        LocalDate start = target.getStartDate();
        LocalDate end = target.getEndDate();
        LocalDate today = LocalDate.now();

        // ✅ 방어용: 과거 일정은 ACTIVE로 저장하지 않도록 막기 (원하면 주석 처리해도 됨)
        if (start.isBefore(today)) {
            log.warn("과거 일정 저장 시도: itineraryId={}, start={}, today={}",
                    target.getId(), start, today);
            throw new IllegalArgumentException("이미 지난 일정은 저장(활성화)할 수 없습니다.");
        }

        // 1) 같은 유저의 ACTIVE 일정 중, 날짜 겹치는 일정들 찾기 (자기 자신 제외)
        List<Itinerary> overlaps =
                itineraryRepository.findByUserIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(
                        req.getUserId(),
                        ItineraryStatus.ACTIVE,
                        end,
                        start,
                        target.getId()
                );

        // 2) 겹치는 일정이 있는데 force=false → 먼저 알림을 던져야 함
        if (!overlaps.isEmpty() && !req.isForce()) {
            return ItinerarySaveResponseDto.builder()
                    .status("OVERLAP")
                    .message("해당 기간에 이미 저장된 일정이 있습니다. 기존 일정을 삭제하고 이 일정으로 업데이트할까요?")
                    .savedItineraryId(null)
                    .deletedIds(
                            overlaps.stream().map(Itinerary::getId).toList()
                    )
                    .build();
        }

        List<Long> deleted = new ArrayList<>();

        // 3) force=true 이고 겹치는 일정이 있으면 → 기존 ACTIVE 일정 삭제(또는 ARCHIVED)
        if (!overlaps.isEmpty() && req.isForce()) {
            for (Itinerary old : overlaps) {
                deleted.add(old.getId());
                itineraryRepository.delete(old);
            }
        }

        // 4) 이제 target 일정을 ACTIVE로 변경
        target.setStatus(ItineraryStatus.ACTIVE);
        itineraryRepository.save(target);

        return ItinerarySaveResponseDto.builder()
                .status("SAVED")
                .message("일정이 저장되었습니다.")
                .savedItineraryId(target.getId())
                .deletedIds(deleted)
                .build();
    }
}
