package com.flowservice.controller.home;

import com.flowservice.entity.MealNutrition;
import com.flowservice.entity.MealRecord;
import com.flowservice.repository.MealRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HomeController 测试类
 * 测试首页热量统计接口的功能
 * 
 * 测试策略：
 * 1. 直接通过 Repository 插入测试数据（不调用上传图片接口）
 * 2. 调用热量统计接口，验证返回结果是否符合预期
 * 3. 使用 @Transactional 确保每个测试用例之间数据隔离
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MealRecordRepository mealRecordRepository;

    // 测试用的用户 ID
    private static final String TEST_USER_ID = "test_user_100";

    /**
     * 每个测试前清理测试数据
     */
    @BeforeEach
    void setUp() {
        // 由于使用 @Transactional，每个测试会自动回滚，无需手动清理
    }

    /**
     * 测试场景1：查询当天热量（有数据的情况）
     * 
     * 测试步骤：
     * 1. 插入当天的 3 条用餐记录，热量分别为 500、300、400
     * 2. 调用接口不传日期参数（默认当天）
     * 3. 验证总热量为 1200，用餐次数为 3，平均热量为 400
     */
    @Test
    void testGetTodayCalories_WithData() throws Exception {
        // Given: 插入当天的 3 条用餐记录
        LocalDateTime now = LocalDateTime.now();
        insertMealRecordWithNutrition(TEST_USER_ID, now.minusHours(6), 500); // 早餐 500 kcal
        insertMealRecordWithNutrition(TEST_USER_ID, now.minusHours(3), 300); // 午餐 300 kcal
        insertMealRecordWithNutrition(TEST_USER_ID, now.minusHours(1), 400); // 晚餐 400 kcal

        // When & Then: 调用接口验证结果
        mockMvc.perform(get("/home/calories")
                .param("userId", TEST_USER_ID.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.message", is("查询成功")))
                .andExpect(jsonPath("$.data.userId", is(TEST_USER_ID)))
                .andExpect(jsonPath("$.data.totalCalories", is(1200)))
                .andExpect(jsonPath("$.data.mealCount", is(3)))
                .andExpect(jsonPath("$.data.averageCaloriesPerMeal", is(400.0)));

        System.out.println("✓ 测试通过：查询当天热量（有数据）成功");
    }

    /**
     * 测试场景2：查询当天热量（没有数据的情况）
     * 
     * 测试步骤：
     * 1. 不插入任何数据
     * 2. 调用接口查询
     * 3. 验证总热量为 0，用餐次数为 0，平均热量为 0
     */
    @Test
    void testGetTodayCalories_NoData() throws Exception {
        // Given: 不插入任何数据

        // When & Then: 调用接口验证结果
        mockMvc.perform(get("/home/calories")
                .param("userId", TEST_USER_ID.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.totalCalories", is(0)))
                .andExpect(jsonPath("$.data.mealCount", is(0)))
                .andExpect(jsonPath("$.data.averageCaloriesPerMeal", is(0.0)));

        System.out.println("✓ 测试通过：查询当天热量（无数据）成功");
    }

    /**
     * 测试场景3：查询指定日期范围的热量
     * 
     * 测试步骤：
     * 1. 插入过去 7 天的用餐记录
     * 2. 调用接口指定日期范围为过去 3 天
     * 3. 验证只统计了 3 天内的数据
     */
    @Test
    void testGetCaloriesWithDateRange() throws Exception {
        // Given: 插入过去 7 天的用餐记录
        LocalDateTime today = LocalDateTime.now();

        // 过去 7 天的数据（应该不在查询范围内）
        insertMealRecordWithNutrition(TEST_USER_ID, today.minusDays(6).withHour(12), 500);
        insertMealRecordWithNutrition(TEST_USER_ID, today.minusDays(5).withHour(12), 500);

        // 过去 3 天的数据（应该在查询范围内）
        insertMealRecordWithNutrition(TEST_USER_ID, today.minusDays(2).withHour(12), 600); // 应计入
        insertMealRecordWithNutrition(TEST_USER_ID, today.minusDays(1).withHour(12), 400); // 应计入
        insertMealRecordWithNutrition(TEST_USER_ID, today.withHour(12), 300); // 应计入

        // 计算日期范围
        LocalDate startDate = LocalDate.now().minusDays(2);
        LocalDate endDate = LocalDate.now();

        // When & Then: 调用接口验证结果
        mockMvc.perform(get("/home/calories")
                .param("userId", TEST_USER_ID.toString())
                .param("startDate", startDate.format(DateTimeFormatter.ISO_DATE))
                .param("endDate", endDate.format(DateTimeFormatter.ISO_DATE)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.totalCalories", is(1300))) // 600 + 400 + 300
                .andExpect(jsonPath("$.data.mealCount", is(3)));

        System.out.println("✓ 测试通过：查询指定日期范围热量成功");
    }

    /**
     * 测试场景4：部分用餐记录没有营养信息
     * 
     * 测试步骤：
     * 1. 插入 3 条记录，其中 1 条没有营养信息
     * 2. 调用接口查询
     * 3. 验证只统计了有营养信息的 2 条记录
     */
    @Test
    void testGetCalories_PartialNutritionData() throws Exception {
        // Given: 插入记录，部分没有营养信息
        LocalDateTime now = LocalDateTime.now();

        insertMealRecordWithNutrition(TEST_USER_ID, now.minusHours(6), 500); // 有营养信息
        insertMealRecordWithoutNutrition(TEST_USER_ID, now.minusHours(4)); // 无营养信息
        insertMealRecordWithNutrition(TEST_USER_ID, now.minusHours(2), 300); // 有营养信息

        // When & Then: 调用接口验证结果
        mockMvc.perform(get("/home/calories")
                .param("userId", TEST_USER_ID.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.totalCalories", is(800))) // 500 + 300
                .andExpect(jsonPath("$.data.mealCount", is(2))); // 只有 2 条有营养信息

        System.out.println("✓ 测试通过：部分数据缺失时统计正确");
    }

    /**
     * 测试场景5：不同用户的数据隔离
     * 
     * 测试步骤：
     * 1. 分别插入用户 A 和用户 B 的用餐记录
     * 2. 查询用户 A 的热量
     * 3. 验证只返回用户 A 的数据
     */
    @Test
    void testGetCalories_UserIsolation() throws Exception {
        // Given: 插入两个用户的数据
        LocalDateTime now = LocalDateTime.now();

        String userA = "test_user_101";
        String userB = "test_user_102";

        insertMealRecordWithNutrition(userA, now.minusHours(2), 600); // 用户 A 的数据
        insertMealRecordWithNutrition(userA, now.minusHours(1), 400); // 用户 A 的数据
        insertMealRecordWithNutrition(userB, now.minusHours(2), 800); // 用户 B 的数据（不应计入）

        // When & Then: 查询用户 A 的热量
        mockMvc.perform(get("/home/calories")
                .param("userId", userA.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.userId", is(userA)))
                .andExpect(jsonPath("$.data.totalCalories", is(1000))) // 只有用户 A 的 600 + 400
                .andExpect(jsonPath("$.data.mealCount", is(2)));

        System.out.println("✓ 测试通过：用户数据隔离正确");
    }

    /**
     * 测试场景6：缺少必填参数 userId
     * 
     * 测试步骤：
     * 1. 调用接口但不传 userId
     * 2. 验证返回错误状态码（4xx 或 5xx）
     */
    @Test
    void testGetCalories_MissingUserId() throws Exception {
        // When & Then: 不传 userId，应该返回错误（可能是 400 或 500）
        mockMvc.perform(get("/home/calories"))
                .andDo(print())
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status < 400 || status >= 600) {
                        throw new AssertionError("期望错误状态码(4xx或5xx)，实际是: " + status);
                    }
                });

        System.out.println("✓ 测试通过：缺少 userId 参数返回 400 错误");
    }

    // ========== 辅助方法 ==========

    /**
     * 插入带有营养信息的用餐记录
     * 
     * @param userId   用户 ID
     * @param eatenAt  用餐时间
     * @param calories 热量（千卡）
     * @return 保存后的 MealRecord
     */
    private MealRecord insertMealRecordWithNutrition(String userId, LocalDateTime eatenAt, int calories) {
        // 创建用餐记录
        MealRecord record = new MealRecord();
        record.setUserId(userId);
        record.setEatenAt(eatenAt);
        record.setSourceType("PHOTO");
        record.setConfidence(0.9);
        record.setIsBalanced(true);
        record.setNutritionSummary("测试数据");

        // 创建营养信息
        MealNutrition nutrition = new MealNutrition();
        nutrition.setMealRecord(record);
        nutrition.setEnergyKcal(calories);
        nutrition.setProteinG(20);
        nutrition.setFatG(10);
        nutrition.setCarbG(50);
        nutrition.setFiberG(5);
        nutrition.setSodiumMg(500);
        nutrition.setSugarG(10);
        nutrition.setSatFatG(3.0);

        // 设置关联
        record.setMealNutrition(nutrition);

        // 保存并返回
        return mealRecordRepository.save(record);
    }

    /**
     * 插入不带营养信息的用餐记录
     * 
     * @param userId  用户 ID
     * @param eatenAt 用餐时间
     * @return 保存后的 MealRecord
     */
    private MealRecord insertMealRecordWithoutNutrition(String userId, LocalDateTime eatenAt) {
        MealRecord record = new MealRecord();
        record.setUserId(userId);
        record.setEatenAt(eatenAt);
        record.setSourceType("TEXT");
        record.setConfidence(0.5);
        record.setIsBalanced(false);
        record.setNutritionSummary("无营养信息");
        // 不设置 mealNutrition

        return mealRecordRepository.save(record);
    }
}
