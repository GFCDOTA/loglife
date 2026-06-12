package com.loglife.nutrition.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link FoodLogJpaEntity}. Infrastructure detail behind the
 * {@link com.loglife.nutrition.application.port.out.FoodLogRepository} port.
 */
public interface SpringDataFoodLogRepository extends JpaRepository<FoodLogJpaEntity, UUID> {

    List<FoodLogJpaEntity> findByLogDateOrderByCreatedAtAsc(LocalDate logDate);
}
