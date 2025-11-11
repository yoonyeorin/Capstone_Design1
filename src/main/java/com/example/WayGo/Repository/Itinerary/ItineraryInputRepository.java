package com.example.WayGo.Repository.Itinerary;

import com.example.WayGo.Entity.ItineraryInput;
import com.example.WayGo.Entity.UserEntity;
import com.example.WayGo.Entity.enums.InputStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ItineraryInput 데이터베이스 접근 인터페이스
 *
 * ⚠️ ID 타입: Integer
 */
@Repository
public interface ItineraryInputRepository extends JpaRepository<ItineraryInput, Integer> {  // Long → Integer

    List<ItineraryInput> findByUser(UserEntity user);

    List<ItineraryInput> findByUserAndStatus(UserEntity user, InputStatus status);

    Optional<ItineraryInput> findByIdAndUser(Integer id, UserEntity user);  // Long → Integer

    @Query("""
        SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
        FROM ItineraryInput i
        WHERE i.user = :user
        AND i.status != 'IN_PROGRESS'
        AND (
            (i.startDate <= :endDate AND i.endDate >= :startDate)
        )
    """)
    boolean existsByUserAndDateRangeOverlap(
            @Param("user") UserEntity user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
        FROM ItineraryInput i
        WHERE i.user = :user
        AND i.id != :excludeId
        AND i.status != 'IN_PROGRESS'
        AND (
            (i.startDate <= :endDate AND i.endDate >= :startDate)
        )
    """)
    boolean existsByUserAndDateRangeOverlapExcluding(
            @Param("user") UserEntity user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Integer excludeId  // Long → Integer
    );

    long countByUser(UserEntity user);

    @Query("""
        SELECT i FROM ItineraryInput i
        WHERE i.user = :user
        AND i.startDate >= :startDate
        AND i.endDate <= :endDate
        ORDER BY i.startDate ASC
    """)
    List<ItineraryInput> findByUserAndDateRange(
            @Param("user") UserEntity user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}