package com.loglife.nutrition.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataUserGoalRepository extends JpaRepository<UserGoalJpaEntity, Short> {
}
