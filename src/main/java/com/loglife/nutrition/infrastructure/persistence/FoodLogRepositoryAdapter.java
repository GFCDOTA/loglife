package com.loglife.nutrition.infrastructure.persistence;

import com.loglife.nutrition.application.port.out.FoodLogRepository;
import com.loglife.nutrition.domain.FoodLog;
import com.loglife.nutrition.domain.FrequentFood;
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
    public List<FoodLog> findByDateBetween(LocalDate start, LocalDate end) {
        return jpa.findByLogDateBetweenOrderByLogDateAscCreatedAtAsc(start, end).stream()
                .map(FoodLogMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<FoodLog> findById(UUID id) {
        return jpa.findById(id).map(FoodLogMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FrequentFood> findFrequentSince(LocalDate since, int limit) {
        // Two-step on purpose: an aggregate for the top names, then the latest row per name.
        // limit is small (UI chips), so the N+1 here is a handful of indexed point queries.
        return jpa.countFrequentNamesSince(since, limit).stream()
                .map(row -> {
                    String name = (String) row[0];
                    int count = ((Number) row[1]).intValue();
                    FoodLogJpaEntity latest = jpa
                            .findFirstByNormalizedFoodNameAndLogDateGreaterThanEqualOrderByCreatedAtDesc(
                                    name, since);
                    return new FrequentFood(FoodLogMapper.toDomain(latest), count);
                })
                .toList();
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
