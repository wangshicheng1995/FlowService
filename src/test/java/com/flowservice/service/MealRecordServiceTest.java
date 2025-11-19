package com.flowservice.service;

import com.flowservice.entity.MealRecord;
import com.flowservice.model.FoodAnalysisResponse;
import com.flowservice.repository.MealRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MealRecordService 测试类
 * 测试用餐记录的插入和查询功能
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MealRecordServiceTest {

    @Autowired
    private MealRecordService mealRecordService;

    @Autowired
    private MealRecordRepository mealRecordRepository;

    /**
     * 测试保存用餐记录 - 营养均衡的案例
     */
    @Test
    void testSaveMealRecord_Balanced() {
        // Given: 创建一个营养均衡的食物分析结果
        FoodAnalysisResponse analysisResponse = new FoodAnalysisResponse();
        analysisResponse.setFoodItems(Arrays.asList("牛油果", "全麦吐司", "鸡蛋"));
        analysisResponse.setConfidence(0.95);
        analysisResponse.setIsBalanced(true);
        analysisResponse.setNutritionSummary("富含健康脂肪和纤维的均衡膳食。");

        // When: 保存用餐记录
        MealRecord savedRecord = mealRecordService.saveMealRecord(analysisResponse, null);

        // Then: 验证保存成功
        assertNotNull(savedRecord.getId(), "记录 ID 不应为空");
        assertEquals(MealRecordService.getDefaultUserId(), savedRecord.getUserId(), "用户 ID 应为默认值");
        assertEquals("PHOTO", savedRecord.getSourceType(), "来源类型应为 PHOTO");
        assertNotNull(savedRecord.getEatenAt(), "用餐时间不应为空");
        assertNotNull(savedRecord.getCreatedAt(), "创建时间不应为空");

        // 验证 AI 分析字段
        assertEquals(0.95, savedRecord.getConfidence(), "确定度应为 0.95");
        assertTrue(savedRecord.getIsBalanced(), "应为营养均衡");
        assertEquals("富含健康脂肪和纤维的均衡膳食。", savedRecord.getNutritionSummary());

        // 验证健康分数和风险等级
        assertNotNull(savedRecord.getHealthScore(), "健康分数不应为空");
        assertTrue(savedRecord.getHealthScore() >= 70, "均衡膳食的健康分数应 >= 70");
        assertEquals("LOW", savedRecord.getRiskLevel(), "风险等级应为 LOW");

        // 验证 JSON 字段不为空
        assertNotNull(savedRecord.getFoodItems(), "食物列表 JSON 不应为空");
        assertNotNull(savedRecord.getAiResultJson(), "AI 结果 JSON 不应为空");

        System.out.println("✓ 测试通过：保存营养均衡的用餐记录成功");
        System.out.println("  记录 ID: " + savedRecord.getId());
        System.out.println("  健康分数: " + savedRecord.getHealthScore());
        System.out.println("  风险等级: " + savedRecord.getRiskLevel());
    }

    /**
     * 测试保存用餐记录 - 营养不均衡的案例
     */
    @Test
    void testSaveMealRecord_Unbalanced() {
        // Given: 创建一个营养不均衡的食物分析结果
        FoodAnalysisResponse analysisResponse = new FoodAnalysisResponse();
        analysisResponse.setFoodItems(Arrays.asList("白米饭"));
        analysisResponse.setConfidence(0.98);
        analysisResponse.setIsBalanced(false);
        analysisResponse.setNutritionSummary("缺少蛋白质和蔬菜，建议搭配菜品。");

        // When: 保存用餐记录
        MealRecord savedRecord = mealRecordService.saveMealRecord(analysisResponse, null);

        // Then: 验证保存成功
        assertNotNull(savedRecord.getId());
        assertFalse(savedRecord.getIsBalanced(), "应为营养不均衡");

        // 验证健康分数较低（因为不均衡）
        assertNotNull(savedRecord.getHealthScore());
        assertTrue(savedRecord.getHealthScore() < 70, "不均衡膳食的健康分数应 < 70");

        System.out.println("✓ 测试通过：保存营养不均衡的用餐记录成功");
        System.out.println("  记录 ID: " + savedRecord.getId());
        System.out.println("  健康分数: " + savedRecord.getHealthScore());
        System.out.println("  风险等级: " + savedRecord.getRiskLevel());
    }

    /**
     * 测试保存用餐记录 - 低确定度案例
     */
    @Test
    void testSaveMealRecord_LowConfidence() {
        // Given: 创建一个低确定度的食物分析结果
        FoodAnalysisResponse analysisResponse = new FoodAnalysisResponse();
        analysisResponse.setFoodItems(Arrays.asList("绿叶蔬菜"));
        analysisResponse.setConfidence(0.65);
        analysisResponse.setIsBalanced(false);
        analysisResponse.setNutritionSummary("图片不清晰，建议重新拍照。");

        // When: 保存用餐记录
        MealRecord savedRecord = mealRecordService.saveMealRecord(analysisResponse, null);

        // Then: 验证保存成功
        assertNotNull(savedRecord.getId());
        assertEquals(0.65, savedRecord.getConfidence());

        // 低确定度 + 不均衡，健康分数应该很低
        assertTrue(savedRecord.getHealthScore() < 50, "低确定度且不均衡的健康分数应 < 50");
        assertEquals("HIGH", savedRecord.getRiskLevel(), "风险等级应为 HIGH");

        System.out.println("✓ 测试通过：保存低确定度的用餐记录成功");
        System.out.println("  记录 ID: " + savedRecord.getId());
        System.out.println("  健康分数: " + savedRecord.getHealthScore());
        System.out.println("  风险等级: " + savedRecord.getRiskLevel());
    }

    /**
     * 测试根据用户 ID 查询用餐记录
     */
    @Test
    void testGetMealRecordsByUserId() {
        // Given: 保存多条用餐记录
        FoodAnalysisResponse response1 = createSampleResponse("苹果", 0.9, true);
        FoodAnalysisResponse response2 = createSampleResponse("汉堡", 0.85, false);
        FoodAnalysisResponse response3 = createSampleResponse("沙拉", 0.95, true);

        mealRecordService.saveMealRecord(response1, null);
        mealRecordService.saveMealRecord(response2, null);
        mealRecordService.saveMealRecord(response3, null);

        // When: 查询默认用户的所有记录
        List<MealRecord> records = mealRecordService.getMealRecordsByUserId(
                MealRecordService.getDefaultUserId());

        // Then: 验证查询结果
        assertNotNull(records, "查询结果不应为空");
        assertTrue(records.size() >= 3, "至少应有 3 条记录");

        System.out.println("✓ 测试通过：根据用户 ID 查询用餐记录成功");
        System.out.println("  查询到 " + records.size() + " 条记录");
    }

    /**
     * 测试根据 ID 查询单条用餐记录
     */
    @Test
    void testGetMealRecordById() {
        // Given: 保存一条用餐记录
        FoodAnalysisResponse analysisResponse = createSampleResponse("鸡胸肉", 0.92, true);
        MealRecord savedRecord = mealRecordService.saveMealRecord(analysisResponse, null);

        // When: 根据 ID 查询
        MealRecord foundRecord = mealRecordService.getMealRecordById(savedRecord.getId());

        // Then: 验证查询结果
        assertNotNull(foundRecord, "查询结果不应为空");
        assertEquals(savedRecord.getId(), foundRecord.getId(), "记录 ID 应匹配");
        assertEquals(0.92, foundRecord.getConfidence(), "确定度应匹配");
        assertTrue(foundRecord.getIsBalanced(), "营养均衡标志应匹配");

        System.out.println("✓ 测试通过：根据 ID 查询用餐记录成功");
        System.out.println("  记录 ID: " + foundRecord.getId());
    }

    /**
     * 测试计算平均健康分数
     */
    @Test
    void testCalculateAverageHealthScore() {
        // Given: 保存多条不同健康分数的记录
        FoodAnalysisResponse response1 = createSampleResponse("健康餐1", 0.9, true);  // 高分
        FoodAnalysisResponse response2 = createSampleResponse("健康餐2", 0.85, true); // 高分
        FoodAnalysisResponse response3 = createSampleResponse("普通餐", 0.7, false);  // 低分

        mealRecordService.saveMealRecord(response1, null);
        mealRecordService.saveMealRecord(response2, null);
        mealRecordService.saveMealRecord(response3, null);

        // When: 计算平均健康分数
        Double avgScore = mealRecordService.getAverageHealthScore(
                MealRecordService.getDefaultUserId());

        // Then: 验证计算结果
        assertNotNull(avgScore, "平均分数不应为空");
        assertTrue(avgScore > 0, "平均分数应大于 0");

        System.out.println("✓ 测试通过：计算平均健康分数成功");
        System.out.println("  平均健康分数: " + avgScore);
    }

    /**
     * 测试统计营养均衡比例
     */
    @Test
    void testGetBalancedMealRatio() {
        // Given: 保存 3 条均衡 + 2 条不均衡的记录
        for (int i = 0; i < 3; i++) {
            FoodAnalysisResponse balanced = createSampleResponse("均衡餐" + i, 0.9, true);
            mealRecordService.saveMealRecord(balanced, null);
        }
        for (int i = 0; i < 2; i++) {
            FoodAnalysisResponse unbalanced = createSampleResponse("不均衡餐" + i, 0.8, false);
            mealRecordService.saveMealRecord(unbalanced, null);
        }

        // When: 计算均衡比例
        double ratio = mealRecordService.getBalancedMealRatio(
                MealRecordService.getDefaultUserId());

        // Then: 验证计算结果 (3/5 = 0.6)
        assertTrue(ratio >= 0.5 && ratio <= 0.7, "均衡比例应在 0.5-0.7 之间");

        System.out.println("✓ 测试通过：统计营养均衡比例成功");
        System.out.println("  营养均衡比例: " + String.format("%.2f%%", ratio * 100));
    }

    /**
     * 测试验证数据持久化
     */
    @Test
    void testDataPersistence() {
        // Given: 保存一条记录
        FoodAnalysisResponse analysisResponse = createSampleResponse("测试食物", 0.88, true);
        MealRecord savedRecord = mealRecordService.saveMealRecord(analysisResponse, null);
        Long recordId = savedRecord.getId();

        // When: 直接从 Repository 查询（绕过 Service 缓存）
        MealRecord persistedRecord = mealRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("记录未找到"));

        // Then: 验证数据已持久化
        assertNotNull(persistedRecord, "持久化记录不应为空");
        assertEquals(recordId, persistedRecord.getId(), "记录 ID 应匹配");
        assertEquals(0.88, persistedRecord.getConfidence(), "确定度应匹配");
        assertNotNull(persistedRecord.getCreatedAt(), "创建时间应已保存");
        assertNotNull(persistedRecord.getUpdatedAt(), "更新时间应已保存");

        System.out.println("✓ 测试通过：数据持久化验证成功");
        System.out.println("  记录已成功保存到数据库");
    }

    // ========== 辅助方法 ==========

    /**
     * 创建示例分析响应
     */
    private FoodAnalysisResponse createSampleResponse(String food, double confidence, boolean isBalanced) {
        FoodAnalysisResponse response = new FoodAnalysisResponse();
        response.setFoodItems(Arrays.asList(food));
        response.setConfidence(confidence);
        response.setIsBalanced(isBalanced);
        response.setNutritionSummary(isBalanced ? "营养均衡" : "营养不均衡");
        return response;
    }
}
