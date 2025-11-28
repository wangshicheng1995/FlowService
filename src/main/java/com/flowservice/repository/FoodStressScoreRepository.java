package com.flowservice.repository;

import com.flowservice.entity.FoodStressScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface FoodStressScoreRepository extends JpaRepository<FoodStressScore, Long> {

    Optional<FoodStressScore> findByUserIdAndScoreDays(Long userId, LocalDate scoreDays);
}
