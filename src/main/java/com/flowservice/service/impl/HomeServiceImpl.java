package com.flowservice.service.impl;

import com.flowservice.entity.MealNutrition;
import com.flowservice.entity.MealRecord;
import com.flowservice.model.CalorieStatisticsResponse;
import com.flowservice.repository.MealRecordRepository;
import com.flowservice.service.HomeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 首页服务实现类
 * 实现用户首页相关的业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final MealRecordRepository mealRecordRepository;

    /**
     * 获取用户在指定时间范围内的食物总热量
     * 
     * 实现逻辑：
     * 1. 如果未传入日期参数，默认使用当天的日期范围（00:00:00 到 23:59:59）
     * 2. 将日期转换为 LocalDateTime，查询该时间范围内用户的所有用餐记录
     * 3. 遍历每条用餐记录，获取关联的营养信息（MealNutrition），累加热量
     * 4. 如果某条记录没有营养信息或热量字段为空，则跳过该记录
     * 5. 计算平均每餐热量（总热量 / 就餐次数），避免除以零的情况
     * 6. 封装统计结果返回
     *
     * @param userId    用户 ID，不能为空
     * @param startDate 开始日期（可选，默认为当天）
     * @param endDate   结束日期（可选，默认为当天）
     * @return 热量统计结果，包含总热量、就餐次数、平均热量等信息
     */
    @Override
    public CalorieStatisticsResponse getTotalCalories(String userId, LocalDate startDate, LocalDate endDate) {
        log.info("开始获取用户热量统计: userId={}, startDate={}, endDate={}", userId, startDate, endDate);

        // 如果未传入日期，默认使用当天
        LocalDate effectiveStartDate = (startDate != null) ? startDate : LocalDate.now();
        LocalDate effectiveEndDate = (endDate != null) ? endDate : LocalDate.now();

        // 将日期转换为 LocalDateTime，设置开始时间为 00:00:00，结束时间为 23:59:59.999999999
        LocalDateTime startTime = effectiveStartDate.atStartOfDay();
        LocalDateTime endTime = effectiveEndDate.atTime(LocalTime.MAX);

        log.debug("查询时间范围: startTime={}, endTime={}", startTime, endTime);

        // 从数据库查询该时间范围内用户的所有用餐记录
        List<MealRecord> mealRecords = mealRecordRepository.findByUserIdAndEatenAtBetween(userId, startTime, endTime);

        log.debug("查询到 {} 条用餐记录", mealRecords.size());

        // 累加总热量，只统计有营养信息且热量不为空的记录
        int totalCalories = 0;
        int validMealCount = 0;

        for (MealRecord record : mealRecords) {
            MealNutrition nutrition = record.getMealNutrition();
            // 检查是否有营养信息且热量字段不为空
            if (nutrition != null && nutrition.getEnergyKcal() != null) {
                totalCalories += nutrition.getEnergyKcal();
                validMealCount++;
                log.trace("累计热量: mealId={}, energyKcal={}, currentTotal={}",
                        record.getId(), nutrition.getEnergyKcal(), totalCalories);
            } else {
                log.debug("跳过无热量信息的记录: mealId={}", record.getId());
            }
        }

        // 计算平均每餐热量，避免除以零
        Double averageCaloriesPerMeal = (validMealCount > 0)
                ? (double) totalCalories / validMealCount
                : 0.0;

        log.info("热量统计完成: userId={}, totalCalories={}, mealCount={}, avgPerMeal={}",
                userId, totalCalories, validMealCount, averageCaloriesPerMeal);

        // 封装并返回统计结果
        return new CalorieStatisticsResponse(
                userId,
                startTime,
                endTime,
                totalCalories,
                validMealCount,
                averageCaloriesPerMeal);
    }
}
