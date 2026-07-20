package com.loglife.nutrition.application.usecase;

import com.loglife.nutrition.application.port.out.FoodLogRepository;
import com.loglife.nutrition.domain.FrequentFood;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Use case: the foods the user logs most often in the recent window — the candidates for
 * one-tap "repeat" (which clones persisted nutrition, no LLM).
 */
public class GetFrequentFoods {

    private final FoodLogRepository repository;

    public GetFrequentFoods(FoodLogRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /** {@code reference} is the client's "today" (dates always come from the client). */
    public record Query(LocalDate reference, int days, int limit) {

        public Query {
            Objects.requireNonNull(reference, "reference");
            if (days < 1) {
                throw new IllegalArgumentException("days must be at least 1");
            }
            if (limit < 1) {
                throw new IllegalArgumentException("limit must be at least 1");
            }
        }
    }

    public List<FrequentFood> handle(Query query) {
        Objects.requireNonNull(query, "query");
        return repository.findFrequentSince(query.reference().minusDays(query.days()), query.limit());
    }
}
