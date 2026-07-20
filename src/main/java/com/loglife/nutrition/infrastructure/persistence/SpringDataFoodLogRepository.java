package com.loglife.nutrition.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link FoodLogJpaEntity}. Infrastructure detail behind the
 * {@link com.loglife.nutrition.application.port.out.FoodLogRepository} port.
 */
public interface SpringDataFoodLogRepository extends JpaRepository<FoodLogJpaEntity, UUID> {

    List<FoodLogJpaEntity> findByLogDateOrderByCreatedAtAsc(LocalDate logDate);

    List<FoodLogJpaEntity> findByLogDateBetweenOrderByLogDateAscCreatedAtAsc(LocalDate start, LocalDate end);

    /**
     * The most-logged food names since {@code since}, most frequent first (ties broken by
     * recency). Each row is {@code [normalized_food_name, times_logged]}.
     */
    @Query(value = """
            SELECT normalized_food_name, COUNT(*) AS times_logged
            FROM food_logs
            WHERE log_date >= :since AND normalized_food_name IS NOT NULL
            GROUP BY normalized_food_name
            ORDER BY times_logged DESC, MAX(created_at) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> countFrequentNamesSince(@Param("since") LocalDate since, @Param("limit") int limit);

    /** The latest log of one food name inside the window — the entry a "repeat" clones. */
    FoodLogJpaEntity findFirstByNormalizedFoodNameAndLogDateGreaterThanEqualOrderByCreatedAtDesc(
            String normalizedFoodName, LocalDate since);
}
