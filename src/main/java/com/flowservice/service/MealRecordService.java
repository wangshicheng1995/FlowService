package com.flowservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowservice.entity.MealRecord;
import com.flowservice.model.FoodAnalysisResponse;
import com.flowservice.repository.MealRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用餐记录服务类
 * 处理用餐记录的业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MealRecordService {

    private final MealRecordRepository mealRecordRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 默认用户 ID（前期 hardcode）
    private static final String DEFAULT_USER_ID = "default_user";

    /**
     * 保存用餐记录
     * 将 AI 分析结果存储到数据库
     *
     * @param analysisResponse AI 分析结果
     * @param imageUrl         图片 URL（可选）
     * @param userId           用户 ID（可选，为空时使用默认值）
     * @return 保存后的用餐记录
     */
    @Transactional
    public MealRecord saveMealRecord(FoodAnalysisResponse analysisResponse, String imageUrl, String userId) {
        try {
            MealRecord record = new MealRecord();

            // 设置基本字段（如果 userId 为空则使用默认值）
            record.setUserId(userId != null && !userId.trim().isEmpty() ? userId : DEFAULT_USER_ID);
            record.setEatenAt(LocalDateTime.now());
            record.setSourceType("PHOTO");
            record.setImageUrl(imageUrl);

            // 设置 AI 分析结果字段
            record.setFoodItems(objectMapper.writeValueAsString(analysisResponse.getFoods()));
            record.setConfidence(analysisResponse.getConfidence());
            record.setIsBalanced(analysisResponse.getIsBalanced());
            record.setNutritionSummary(analysisResponse.getNutritionSummary());

            // 计算健康分数（基于确定度和营养均衡度）
            int healthScore = calculateHealthScore(analysisResponse);
            record.setHealthScore(healthScore);

            // 计算风险等级
            String riskLevel = calculateRiskLevel(analysisResponse, healthScore);
            record.setRiskLevel(riskLevel);

            // 保存完整的 AI 返回 JSON
            record.setAiResultJson(objectMapper.writeValueAsString(analysisResponse));

            // 设置营养详细信息
            if (analysisResponse.getNutrition() != null) {
                com.flowservice.entity.MealNutrition nutrition = new com.flowservice.entity.MealNutrition();
                nutrition.setMealRecord(record);
                nutrition.setEnergyKcal(analysisResponse.getNutrition().getEnergyKcal());
                nutrition.setProteinG(analysisResponse.getNutrition().getProteinG());
                nutrition.setFatG(analysisResponse.getNutrition().getFatG());
                nutrition.setCarbG(analysisResponse.getNutrition().getCarbG());
                nutrition.setFiberG(analysisResponse.getNutrition().getFiberG());
                nutrition.setSodiumMg(analysisResponse.getNutrition().getSodiumMg());
                nutrition.setSugarG(analysisResponse.getNutrition().getSugarG());
                nutrition.setSatFatG(analysisResponse.getNutrition().getSatFatG());

                // 保存优质蛋白列表（序列化为 JSON）
                if (analysisResponse.getHighQualityProteins() != null
                        && !analysisResponse.getHighQualityProteins().isEmpty()) {
                    nutrition.setHighQualityProteins(
                            objectMapper.writeValueAsString(analysisResponse.getHighQualityProteins()));
                }

                record.setMealNutrition(nutrition);
            }

            MealRecord savedRecord = mealRecordRepository.save(record);
            log.info("用餐记录已保存: id={}, userId={}, healthScore={}, riskLevel={}",
                    savedRecord.getId(), savedRecord.getUserId(), savedRecord.getHealthScore(),
                    savedRecord.getRiskLevel());

            return savedRecord;

        } catch (JsonProcessingException e) {
            log.error("JSON 序列化失败", e);
            throw new RuntimeException("保存用餐记录失败: JSON 序列化错误", e);
        }
    }

    /**
     * 根据用户 ID 查询所有用餐记录
     *
     * @param userId 用户 ID
     * @return 用餐记录列表
     */
    public List<MealRecord> getMealRecordsByUserId(String userId) {
        log.info("查询用户的用餐记录: userId={}", userId);
        return mealRecordRepository.findByUserIdOrderByEatenAtDesc(userId);
    }

    /**
     * 查询默认用户的所有用餐记录
     *
     * @return 用餐记录列表
     */
    public List<MealRecord> getDefaultUserMealRecords() {
        return getMealRecordsByUserId(DEFAULT_USER_ID);
    }

    /**
     * 根据 ID 查询用餐记录
     *
     * @param id 记录 ID
     * @return 用餐记录
     */
    public MealRecord getMealRecordById(Long id) {
        return mealRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用餐记录不存在: id=" + id));
    }

    /**
     * 查询用户的平均健康分数
     *
     * @param userId 用户 ID
     * @return 平均健康分数
     */
    public Double getAverageHealthScore(String userId) {
        Double avgScore = mealRecordRepository.calculateAverageHealthScore(userId);
        log.info("用户平均健康分数: userId={}, avgScore={}", userId, avgScore);
        return avgScore != null ? avgScore : 0.0;
    }

    /**
     * 查询用户最近的用餐记录
     *
     * @param userId 用户 ID
     * @param limit  记录数量
     * @return 用餐记录列表
     */
    public List<MealRecord> getRecentMealRecords(String userId, int limit) {
        log.info("查询用户最近的用餐记录: userId={}, limit={}", userId, limit);
        return mealRecordRepository.findRecentMealsByUserId(userId, limit);
    }

    /**
     * 统计用户的用餐记录总数
     *
     * @param userId 用户 ID
     * @return 记录总数
     */
    public long countMealRecords(String userId) {
        return mealRecordRepository.countByUserId(userId);
    }

    /**
     * 统计用户营养均衡的用餐比例
     *
     * @param userId 用户 ID
     * @return 均衡比例（0-1）
     */
    public double getBalancedMealRatio(String userId) {
        long totalCount = mealRecordRepository.countByUserId(userId);
        if (totalCount == 0) {
            return 0.0;
        }
        long balancedCount = mealRecordRepository.countByUserIdAndIsBalanced(userId, true);
        return (double) balancedCount / totalCount;
    }

    /**
     * 计算健康分数
     * 基于识别确定度和营养均衡度
     *
     * @param analysisResponse AI 分析结果
     * @return 健康分数（0-100）
     */
    private int calculateHealthScore(FoodAnalysisResponse analysisResponse) {
        double confidence = analysisResponse.getConfidence() != null ? analysisResponse.getConfidence() : 0.5;
        boolean isBalanced = analysisResponse.getIsBalanced() != null && analysisResponse.getIsBalanced();

        // 基础分：确定度 * 50
        double baseScore = confidence * 50;

        // 营养均衡加分：50 分
        double balanceBonus = isBalanced ? 50 : 0;

        // 总分
        int totalScore = (int) Math.round(baseScore + balanceBonus);

        // 确保分数在 0-100 之间
        return Math.max(0, Math.min(100, totalScore));
    }

    /**
     * 计算风险等级
     *
     * @param analysisResponse AI 分析结果
     * @param healthScore      健康分数
     * @return 风险等级（LOW/MEDIUM/HIGH）
     */
    private String calculateRiskLevel(FoodAnalysisResponse analysisResponse, int healthScore) {
        // 健康分数 >= 70: LOW
        // 健康分数 40-69: MEDIUM
        // 健康分数 < 40: HIGH

        if (healthScore >= 70) {
            return "LOW";
        } else if (healthScore >= 40) {
            return "MEDIUM";
        } else {
            return "HIGH";
        }
    }

    /**
     * 获取默认用户 ID
     *
     * @return 默认用户 ID
     */
    public static String getDefaultUserId() {
        return DEFAULT_USER_ID;
    }
}
