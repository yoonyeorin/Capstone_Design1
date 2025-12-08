package com.example.WayGo.Entity.Itinerary;


import com.example.WayGo.Entity.enums.ItineraryStatus;
import com.example.WayGo.Entity.enums.ScheduleDensity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;


@Entity
@Table(name = "itineraries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Itinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 50)
    private String destinationCity;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Integer totalDays;

    @Column
    private Integer totalBudget;

    @Column
    private Integer totalSpent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItineraryStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_density", nullable = false, length = 20)
    private ScheduleDensity scheduleDensity;

    @Lob
    @Column(name = "ai_raw_text", columnDefinition = "LONGTEXT")
    private String aiRawText;

    @Lob
    @Column(name = "ai_json", columnDefinition = "LONGTEXT")
    private String aiJson;

    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<ItineraryDay> days;
}
