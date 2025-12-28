package com.flowservice.service.impl;

import com.flowservice.entity.MealNutrition;
import com.flowservice.entity.MealRecord;
import com.flowservice.model.DailyCalorieResponse;
import com.flowservice.repository.MealRecordRepository;
import com.flowservice.service.SummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Summary 页面服务实现类
 * 实现统计概览相关的业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryServiceImpl implements SummaryService {

    private final MealRecordRepository mealRecordRepository;

    /**
     * 获取用户在指定时间范围内的每日热量明细
     * 用于柱状图展示
     *
     * 实现逻辑：
     * 1. 如果未传入日期参数，默认查询最近 7 天（包括今天）
     * 2. 遍历日期范围内的每一天
     * 3. 对每一天查询该用户的所有用餐记录，累加热量
     * 4. 返回每日热量列表，按日期升序排列
     *
     * @param userId    用户 ID
     * @param startDate 开始日期（可选，默认为 7 天前）
     * @param endDate   结束日期（可选，默认为今天）
     * @return 每日热量列表
     */
    @Override
    public List<DailyCalorieResponse> getDailyCalories(String userId, LocalDate startDate, LocalDate endDate) {
        log.info("开始获取用户每日热量明细: userId={}, startDate={}, endDate={}", userId, startDate, endDate);

        // 如果未传入日期，默认查询最近 7 天
        LocalDate effectiveEndDate = (endDate != null) ? endDate : LocalDate.now();
        LocalDate effectiveStartDate = (startDate != null) ? startDate : effectiveEndDate.minusDays(6);

        log.debug("有效日期范围: startDate={}, endDate={}", effectiveStartDate, effectiveEndDate);

        List<DailyCalorieResponse> result = new ArrayList<>();

        // 遍历日期范围内的每一天
        LocalDate currentDate = effectiveStartDate;
        while (!currentDate.isAfter(effectiveEndDate)) {
            DailyCalorieResponse dailyData = calculateDailyCalories(userId, currentDate);
            result.add(dailyData);
            currentDate = currentDate.plusDays(1);
        }

        log.info("每日热量明细获取完成: userId={}, days={}", userId, result.size());
        return result;
    }

    /**
     * 计算某一天的热量统计
     *
     * @param userId 用户 ID
     * @param date   日期
     * @return 当日热量数据
     */
    private DailyCalorieResponse calculateDailyCalories(String userId, LocalDate date) {
        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.atTime(LocalTime.MAX);

        // 查询当天的用餐记录
        List<MealRecord> mealRecords = mealRecordRepository.findByUserIdAndEatenAtBetween(userId, startTime, endTime);

        int totalCalories = 0;
        int validMealCount = 0;

        for (MealRecord record : mealRecords) {
            MealNutrition nutrition = record.getMealNutrition();
            if (nutrition != null && nutrition.getEnergyKcal() != null) {
                totalCalories += nutrition.getEnergyKcal();
                validMealCount++;
            }
        }

        log.trace("日期 {} 热量统计: calories={}, mealCount={}", date, totalCalories, validMealCount);

        return new DailyCalorieResponse(date, totalCalories, validMealCount);
    }
}
