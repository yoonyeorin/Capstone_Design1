package com.example.WayGo.Repository.Itinerary;

import com.example.WayGo.Entity.Itinerary;
import com.example.WayGo.Entity.UserEntity;
import com.example.WayGo.Entity.enums.ItineraryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Itinerary 데이터베이스 접근 인터페이스
 */
@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {

    /**
     * 사용자의 모든 일정 조회
     */
    List<Itinerary> findByUser(UserEntity user);

    /**
     * 사용자의 특정 상태 일정 조회
     */
    List<Itinerary> findByUserAndStatus(UserEntity user, ItineraryStatus status);

    /**
     * 사용자의 일정을 최신순으로 조회
     */
    List<Itinerary> findByUserOrderByCreatedAtDesc(UserEntity user);

    /**
     * 사용자의 활성 일정만 조회 (진행중인 여행)
     */
    @Query("""
        SELECT i FROM Itinerary i
        WHERE i.user = :user
        AND i.status = 'ACTIVE'
        ORDER BY i.createdAt DESC
    """)
    List<Itinerary> findActiveItinerariesByUser(@Param("user") UserEntity user);

    /**
     * 특정 기간에 겹치는 일정 확인
     */
    @Query("""
        SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
        FROM Itinerary i
        JOIN i.itineraryInput input
        WHERE i.user = :user
        AND i.status = 'ACTIVE'
        AND (
            (input.startDate <= :endDate AND input.endDate >= :startDate)
        )
    """)
    boolean existsByUserAndDateRangeOverlap(
            @Param("user") UserEntity user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 사용자의 일정 개수
     */
    long countByUser(UserEntity user);

    /**
     * 특정 입력으로 생성된 일정 조회
     */
    @Query("""
        SELECT i FROM Itinerary i
        WHERE i.itineraryInput.id = :inputId
    """)
    Optional<Itinerary> findByItineraryInputId(@Param("inputId") Long inputId);
}