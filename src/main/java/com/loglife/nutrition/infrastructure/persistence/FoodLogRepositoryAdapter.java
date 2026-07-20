package com.loglife.nutrition.infrastructure.persistence;

import com.loglife.nutrition.application.port.out.FoodLogRepository;
import com.loglife.nutrition.domain.FoodLog;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-backed implementation of the {@link FoodLogRepository} port.
 */
@Repository
public class FoodLogRepositoryAdapter implements FoodLogRepository {

    private final SpringDataFoodLogRepository jpa;

    public FoodLogRepositoryAdapter(SpringDataFoodLogRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public FoodLog save(FoodLog foodLog) {
        return FoodLogMapper.toDomain(jpa.save(FoodLogMapper.toEntity(foodLog)));
    }

    @Override
    @Transactional
    public List<FoodLog> saveAll(List<FoodLog> foodLogs) {
        List<FoodLogJpaEntity> entities = foodLogs.stream().map(FoodLogMapper::toEntity).toList();
        return jpa.saveAll(entities).stream().map(FoodLogMapper::toDomain).toList();
    }

    @Override
    public List<FoodLog> findByDate(LocalDate date) {
        return jpa.findByLogDateOrderByCreatedAtAsc(date).stream()
                .map(FoodLogMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<FoodLog> findById(UUID id) {
        return jpa.findById(id).map(FoodLogMapper::toDomain);
    }

    @Override
    @Transactional
    public boolean deleteById(UUID id) {
        if (!jpa.existsById(id)) {
            return false;
        }
        jpa.deleteById(id);
        return true;
    }
}
