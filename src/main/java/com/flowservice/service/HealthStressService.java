package com.flowservice.service;

import com.flowservice.entity.FoodStressScore;
import com.flowservice.entity.MealRecord;
import com.flowservice.model.NutritionTag;
import com.flowservice.repository.FoodStressScoreRepository;
import com.flowservice.repository.MealRecordRepository;
import com.flowservice.util.HealthTagCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class HealthStressService {

    private final MealRecordRepository mealRecordRepository;
    private final FoodStressScoreRepository foodStressScoreRepository;

    // 风险标签集合
    private static final Set<NutritionTag> RISK_TAGS = Set.of(
            NutritionTag.HIGH_SODIUM,
            NutritionTag.HIGH_SUGAR,
            NutritionTag.LOW_FIBER,
            NutritionTag.HIGH_SAT_FAT,
            NutritionTag.HIGH_ENERGY_DENSE,
            NutritionTag.PROCESSED_MEAT,
            NutritionTag.DEEP_FRIED,
            NutritionTag.SUGARY_DRINK,
            NutritionTag.GENERIC_HIGH_RISK);

    // 保护标签集合
    private static final Set<NutritionTag> PROTECTIVE_TAGS = Set.of(
            NutritionTag.HIGH_FIBER_MEAL,
            NutritionTag.VEGETABLE_RICH,
            NutritionTag.LEAN_PROTEIN,
            NutritionTag.BALANCED_MEAL);

    /**
     * 计算并保存用户当天的健康压力值
     *
     * @param userId 用户ID
     * @param date   日期
     * @return 当天的分数 (0-100)
     */
    @Transactional
    public int calculateDailyScore(Long userId, LocalDate date) {
        // 1. 查询当天的所有用餐记录
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        List<MealRecord> meals = mealRecordRepository.findByUserIdAndEatenAtBetweenOrderByEatenAtDesc(
                userId, startOfDay, endOfDay);

        // 2. 如果没有记录，返回默认值 40
        if (meals.isEmpty()) {
            return saveOrUpdateScore(userId, date, 40);
        }

        // 3. 计算分数
        // 注意：需求要求按时间顺序累加，Repository 返回的是倒序 (Desc)，所以需要反转或者倒序遍历
        // 这里我们简单地反转列表，使其按时间正序排列
        java.util.Collections.reverse(meals);

        int score = 40; // 初始值

        for (MealRecord meal : meals) {
            // 获取该餐的标签
            Set<NutritionTag> tags = HealthTagCalculator.calcTags(meal);

            // 计算 delta
            int delta = calculateDeltaByTags(tags);
            score += delta;

            // 限制范围 0-100
            if (score > 100)
                score = 100;
            if (score < 0)
                score = 0;
        }

        // 4. 保存到数据库
        return saveOrUpdateScore(userId, date, score);
    }

    /**
     * 保存或更新分数
     */
    private int saveOrUpdateScore(Long userId, LocalDate date, int score) {
        Optional<FoodStressScore> existing = foodStressScoreRepository.findByUserIdAndScoreDays(userId, date);
        FoodStressScore entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setScore(score);
        } else {
            entity = new FoodStressScore();
            entity.setUserId(userId);
            entity.setScoreDays(date);
            entity.setScore(score);
        }
        foodStressScoreRepository.save(entity);
        return score;
    }

    /**
     * 根据标签计算单餐的分数变化
     */
    public int calculateDeltaByTags(Set<NutritionTag> tags) {
        int riskCount = 0;
        int protectCount = 0;

        for (NutritionTag tag : tags) {
            if (RISK_TAGS.contains(tag)) {
                riskCount++;
            }
            if (PROTECTIVE_TAGS.contains(tag)) {
                protectCount++;
            }
        }

        int netRisk = riskCount - protectCount;

        if (netRisk >= 3)
            return 20;
        if (netRisk == 2)
            return 15;
        if (netRisk == 1)
            return 10;
        if (netRisk == 0)
            return 0;
        if (netRisk == -1 || netRisk == -2)
            return -10;
        if (netRisk <= -3)
            return -20;

        return 0; // Should not reach here
    }
}
